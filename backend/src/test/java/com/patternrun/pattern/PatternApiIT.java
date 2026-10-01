package com.patternrun.pattern;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.patternrun.support.ApiIntegrationTestBase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class PatternApiIT extends ApiIntegrationTestBase {

    @Test
    @DisplayName("GET /api/v1/patterns returns the ten patterns in learning path order")
    void returnsAllPatterns() throws Exception {
        mockMvc.perform(get("/api/v1/patterns"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(10)))
                .andExpect(jsonPath("$[*].slug", contains(
                        "hashing",
                        "sliding-window",
                        "two-pointers",
                        "binary-search",
                        "intervals",
                        "monotonic-stack",
                        "heap",
                        "bfs-dfs",
                        "trees",
                        "dp")))
                .andExpect(jsonPath("$[0].name", is("Hashing")))
                .andExpect(jsonPath("$[0].difficultyOrder", is(1)))
                .andExpect(jsonPath("$[0].problemCount", is(2)));
    }

    @Test
    @DisplayName("GET /api/v1/patterns/{slug} returns the teaching content")
    void returnsPatternDetail() throws Exception {
        mockMvc.perform(get("/api/v1/patterns/sliding-window"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name", is("Sliding Window")))
                .andExpect(jsonPath("$.signal", containsString("contiguous")))
                .andExpect(jsonPath("$.mentalModel", containsString("RIGHT expands")))
                .andExpect(jsonPath("$.recognitionRules", hasSize(5)))
                .andExpect(jsonPath("$.template[5]", containsString("while window is invalid")))
                .andExpect(jsonPath("$.invariant", containsString("valid after shrinking")))
                .andExpect(jsonPath("$.problemCount", is(2)));
    }

    @Test
    @DisplayName("GET /api/v1/patterns/{slug}/problems lists that pattern's problems")
    void returnsProblemsForPattern() throws Exception {
        mockMvc.perform(get("/api/v1/patterns/binary-search/problems"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[*].slug", contains(
                        "search-in-rotated-sorted-array",
                        "find-minimum-in-rotated-sorted-array")))
                .andExpect(jsonPath("$[0].complexity.time", is("O(log n)")));
    }

    @Test
    @DisplayName("Unknown pattern slug returns a clean 404")
    void unknownPatternReturnsNotFound() throws Exception {
        mockMvc.perform(get("/api/v1/patterns/nope"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message", containsString("nope")));
    }
}