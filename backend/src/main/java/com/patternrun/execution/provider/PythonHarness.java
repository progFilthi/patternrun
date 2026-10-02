package com.patternrun.execution.provider;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

/**
 * The Python runner program, loaded from the classpath rather than inlined.
 *
 * A 200-line Python program embedded in a Java text block would be unreviewable, untestable in
 * isolation, and impossible to run through a Python linter. Keeping it as a real file means it can
 * be exercised directly — which is how the timeout, the bounded-output sink and the error
 * translation were all verified before any of it was wired to a container.
 */
@Component
public class PythonHarness {

    private static final String LOCATION = "code-execution/python-harness.py";

    private final String source;

    public PythonHarness() {
        this.source = read();
    }

    /**
     * The harness source, to be written into the sandbox's filesystem.
     *
     * Written as a file rather than passed on the command line for two reasons: it keeps the
     * command readable, and the learner's code is passed on stdin so it never appears in the
     * process table where another tenant of the host could read it.
     */
    public String source() {
        return source;
    }

    private static String read() {
        ClassPathResource resource = new ClassPathResource(LOCATION);
        if (!resource.exists()) {
            throw new IllegalStateException("Missing " + LOCATION + " from the classpath");
        }
        try (InputStream stream = resource.getInputStream()) {
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException ex) {
            throw new IllegalStateException("Cannot read " + LOCATION, ex);
        }
    }
}