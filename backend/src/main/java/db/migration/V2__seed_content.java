package db.migration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.flywaydb.core.api.migration.Context;

/**
 * Loads the version controlled product content from {@code /seed} into the database.
 *
 * Content lives in JSON rather than in SQL because problem content is product content
 * (README section 65). This migration fails loudly on incomplete content, so a broken
 * problem file can never reach the database.
 */
public class V2__seed_content extends BaseJavaMigration {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final String PATTERN_LOCATION_PATTERN = "classpath*:seed/patterns/*.json";
    private static final String PROBLEM_LOCATION_PATTERN = "classpath*:seed/problems/*.json";
    private static final ResourcePatternResolver RESOURCE_RESOLVER = new PathMatchingResourcePatternResolver();

    @Override
    public void migrate(Context context) throws Exception {
        Connection connection = context.getConnection();

        Map<String, UUID> patternIds = insertPatterns(connection);
        List<JsonNode> problems = readResources(PROBLEM_LOCATION_PATTERN);
        problems.sort((left, right) -> left.get("slug").asText().compareTo(right.get("slug").asText()));
        insertProblems(connection, patternIds, problems);

        System.out.println("PatternRun seed: inserted " + patternIds.size() + " patterns and "
                + problems.size() + " problems");
    }

    private Map<String, UUID> insertPatterns(Connection connection) throws SQLException, IOException {
        List<JsonNode> patterns = readResources(PATTERN_LOCATION_PATTERN);
        patterns.sort((left, right) -> left.get("difficultyOrder").asInt() - right.get("difficultyOrder").asInt());

        Map<String, UUID> ids = new HashMap<>();
        String sql = "INSERT INTO patterns (slug, name, summary, signal, mental_model, recognition_rules,"
                + " template, invariant, difficulty_order) VALUES (?, ?, ?, ?, ?, ?::jsonb, ?::jsonb, ?, ?) RETURNING id";

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            for (JsonNode pattern : patterns) {
                String slug = requireText(pattern, "slug");
                statement.setString(1, slug);
                statement.setString(2, requireText(pattern, "name"));
                statement.setString(3, requireText(pattern, "summary"));
                statement.setString(4, requireText(pattern, "signal"));
                statement.setString(5, requireText(pattern, "mentalModel"));
                statement.setString(6, json(requireArray(pattern, "recognitionRules")));
                statement.setString(7, json(requireArray(pattern, "template")));
                statement.setString(8, requireText(pattern, "invariant"));
                statement.setInt(9, pattern.get("difficultyOrder").asInt());
                try (ResultSet resultSet = statement.executeQuery()) {
                    resultSet.next();
                    ids.put(slug, resultSet.getObject(1, UUID.class));
                }
            }
        }
        return ids;
    }

    private void insertProblems(Connection connection, Map<String, UUID> patternIds, List<JsonNode> problems)
            throws SQLException {
        Set<String> seenSlugs = new HashSet<>();
        for (JsonNode problem : problems) {
            String slug = requireText(problem, "slug");
            if (!seenSlugs.add(slug)) {
                throw new IllegalStateException("Duplicate problem slug in seed content: " + slug);
            }
            UUID primary = requirePattern(patternIds, requireText(problem, "primaryPattern"), slug);

            UUID problemId = insertProblem(connection, problem, primary);
            insertSecondaryPatterns(connection, problemId, patternIds, problem, slug);
            insertExamples(connection, problemId, problem);
            insertHints(connection, problemId, problem);
            insertAnimationSteps(connection, problemId, problem);
            insertTestCases(connection, problemId, problem);
            insertSolutions(connection, problemId, problem);
        }
    }

    private UUID insertProblem(Connection connection, JsonNode problem, UUID primary) throws SQLException {
        String sql = "INSERT INTO problems (external_id, slug, title, difficulty, training_difficulty, statement,"
                + " constraints, why_this_pattern, brute_force, invariant, pseudocode, common_mistakes,"
                + " interview_explanation, time_complexity, space_complexity, primary_pattern_id)"
                + " VALUES (?, ?, ?, ?, ?, ?, ?::jsonb, ?, ?, ?, ?::jsonb, ?::jsonb, ?, ?, ?, ?)"
                + " RETURNING id";

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, problem.get("externalId").asInt());
            statement.setString(2, requireText(problem, "slug"));
            statement.setString(3, requireText(problem, "title"));
            statement.setString(4, requireEnum(problem, "difficulty"));
            statement.setString(5, requireEnum(problem, "trainingDifficulty"));
            statement.setString(6, requireText(problem, "statement"));
            statement.setString(7, json(requireArray(problem, "constraints")));
            statement.setString(8, requireText(problem, "whyThisPattern"));
            statement.setString(9, requireText(problem, "bruteForce"));
            statement.setString(10, requireText(problem, "invariant"));
            statement.setString(11, json(requireArray(problem, "pseudocode")));
            statement.setString(12, json(requireArray(problem, "commonMistakes")));
            statement.setString(13, requireText(problem, "interviewExplanation"));
            statement.setString(14, requireText(problem, "timeComplexity"));
            statement.setString(15, requireText(problem, "spaceComplexity"));
            statement.setObject(16, primary);
            try (ResultSet resultSet = statement.executeQuery()) {
                resultSet.next();
                return resultSet.getObject(1, UUID.class);
            }
        }
    }

    private void insertSecondaryPatterns(Connection connection,
                                         UUID problemId,
                                         Map<String, UUID> patternIds,
                                         JsonNode problem,
                                         String slug) throws SQLException {
        JsonNode secondary = problem.get("secondaryPatterns");
        if (secondary == null || secondary.isEmpty()) {
            return;
        }
        String sql = "INSERT INTO problem_secondary_patterns (problem_id, pattern_id) VALUES (?, ?)";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            for (JsonNode patternSlug : secondary) {
                UUID patternId = requirePattern(patternIds, patternSlug.asText(), slug);
                statement.setObject(1, problemId);
                statement.setObject(2, patternId);
                statement.addBatch();
            }
            statement.executeBatch();
        }
    }

    private void insertExamples(Connection connection, UUID problemId, JsonNode problem) throws SQLException {
        String sql = "INSERT INTO problem_examples (problem_id, ordinal, input, output, explanation)"
                + " VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            int ordinal = 1;
            for (JsonNode example : requireArray(problem, "examples")) {
                statement.setObject(1, problemId);
                statement.setInt(2, ordinal++);
                statement.setString(3, requireText(example, "input"));
                statement.setString(4, requireText(example, "output"));
                statement.setString(5, requireText(example, "explanation"));
                statement.addBatch();
            }
            statement.executeBatch();
        }
    }

    private void insertHints(Connection connection, UUID problemId, JsonNode problem) throws SQLException {
        String sql = "INSERT INTO problem_hints (problem_id, level, content) VALUES (?, ?, ?)";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            for (JsonNode hint : requireArray(problem, "hints")) {
                statement.setObject(1, problemId);
                statement.setInt(2, hint.get("level").asInt());
                statement.setString(3, requireText(hint, "content"));
                statement.addBatch();
            }
            statement.executeBatch();
        }
    }

    private void insertAnimationSteps(Connection connection, UUID problemId, JsonNode problem) throws SQLException {
        String sql = "INSERT INTO problem_animation_steps"
                + " (problem_id, step_order, step_type, title, description, text, payload)"
                + " VALUES (?, ?, ?, ?, ?, ?, ?::jsonb)";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            for (JsonNode step : requireArray(problem, "animationSteps")) {
                JsonNode payload = step.get("payload");
                statement.setObject(1, problemId);
                statement.setInt(2, step.get("order").asInt());
                statement.setString(3, requireText(step, "type"));
                statement.setString(4, requireText(step, "title"));
                statement.setString(5, requireText(step, "description"));
                statement.setString(6, requireText(step, "text"));
                statement.setString(7, payload == null ? "{}" : payload.toString());
                statement.addBatch();
            }
            statement.executeBatch();
        }
    }

    private void insertTestCases(Connection connection, UUID problemId, JsonNode problem) throws SQLException {
        String sql = "INSERT INTO problem_test_cases"
                + " (problem_id, ordinal, label, input_data, expected_output, is_hidden) VALUES (?, ?, ?, ?, ?, ?)";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            int ordinal = 1;
            for (JsonNode testCase : requireArray(problem, "testCases")) {
                statement.setObject(1, problemId);
                statement.setInt(2, ordinal++);
                statement.setString(3, requireText(testCase, "label"));
                statement.setString(4, requireText(testCase, "input"));
                statement.setString(5, requireText(testCase, "expectedOutput"));
                statement.setBoolean(6, testCase.path("hidden").asBoolean(false));
                statement.addBatch();
            }
            statement.executeBatch();
        }
    }

    private void insertSolutions(Connection connection, UUID problemId, JsonNode problem) throws SQLException {
        String sql = "INSERT INTO problem_solutions (problem_id, language, code) VALUES (?, ?, ?)";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            for (JsonNode solution : requireArray(problem, "solutions")) {
                statement.setObject(1, problemId);
                statement.setString(2, requireText(solution, "language"));
                statement.setString(3, requireText(solution, "code"));
                statement.addBatch();
            }
            statement.executeBatch();
        }
    }

    private List<JsonNode> readResources(String locationPattern) throws IOException {
        Resource[] found = RESOURCE_RESOLVER.getResources(locationPattern);
        if (found.length == 0) {
            throw new IllegalStateException("No seed content found for pattern: " + locationPattern);
        }
        List<Resource> resources = new ArrayList<>(List.of(found));
        resources.sort(Comparator.comparing(Resource::getFilename));

        List<JsonNode> nodes = new ArrayList<>(resources.size());
        for (Resource resource : resources) {
            try (InputStream stream = resource.getInputStream()) {
                nodes.add(MAPPER.readTree(stream));
            } catch (IOException ex) {
                throw new UncheckedIOException("Cannot read seed resource " + resource, ex);
            }
        }
        return nodes;
    }

    private static String requireText(JsonNode node, String field) {
        JsonNode value = node.get(field);
        if (value == null || value.isNull() || value.asText().isBlank()) {
            throw new IllegalStateException("Seed content is missing required text field '" + field + "' in "
                    + node.path("slug").asText("unknown content"));
        }
        return value.asText();
    }

    private static String requireEnum(JsonNode node, String field) {
        return requireText(node, field).toUpperCase(java.util.Locale.ROOT);
    }

    private static JsonNode requireArray(JsonNode node, String field) {
        JsonNode value = node.get(field);
        if (value == null || !value.isArray() || value.isEmpty()) {
            throw new IllegalStateException("Seed content is missing required array field '" + field + "' in "
                    + node.path("slug").asText("unknown content"));
        }
        return value;
    }

    private static UUID requirePattern(Map<String, UUID> patternIds, String slug, String problemSlug) {
        UUID id = patternIds.get(slug);
        if (id == null) {
            throw new IllegalStateException(
                    "Problem " + problemSlug + " references unknown pattern slug: " + slug);
        }
        return id;
    }

    private static String json(JsonNode node) {
        return node.toString();
    }
}