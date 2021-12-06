package org.apache.camel.kamelets.test;

import org.apache.camel.dsl.jbang.core.commands.CamelJBangMain;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.WildcardFileFilter;

import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

import org.awaitility.Awaitility;

import java.io.File;
import java.io.FileFilter;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Supplier;

public class JbangService implements BeforeEachCallback, AfterEachCallback {

	public static final String LOG_FILE = "target/app.log";
	private String integration;
	private StringBuilder parameters = new StringBuilder();
	private Supplier<Map<String, Object>> systemProperties;
	private ExecutorService executor = Executors.newFixedThreadPool(1);

	public JbangService(String integration, Supplier<Map<String, Object>> systemProperties, String... parameters) {
		this.integration = Path.of("src", "main", "resources", integration).toAbsolutePath().toString();
		this.parameters.append("--local-kamelet-dir=" + Path.of("").toAbsolutePath().getParent().getParent().toString() + File.separator + "kamelets");
		if (parameters != null && parameters.length > 0) {
			this.parameters.append(" ");
			StringBuilder params = new StringBuilder();
			Arrays.stream(parameters).map(param -> param + " ").forEach(params::append);

			this.parameters.append(params.toString());
		}
		this.systemProperties = systemProperties;
	}

	@Override
	public void afterEach(ExtensionContext extensionContext) throws Exception {
		File kameletTestDir = new File(".");
		FileFilter fileFilter = new WildcardFileFilter(".run*.camel.lock");
		File[] files = kameletTestDir.listFiles(fileFilter);

		for (File file : files) {
			file.delete();
		}
	}

	@Override
	public void beforeEach(ExtensionContext extensionContext) throws Exception {
		FileUtils.write(new File(LOG_FILE), "", Charset.defaultCharset());

		systemProperties.get().forEach((key, value) -> {
			System.setProperty(key, String.valueOf(value));
		});

		Runnable camelJbangRunnable = () -> CamelJBangMain.run("run", integration, parameters.toString());
		executor.execute(camelJbangRunnable);

		Awaitility.await().atMost(Duration.ofSeconds(60)).until(() ->
				FileUtils.readFileToString(
						new File(LOG_FILE), Charset.defaultCharset()).contains("(CamelJBang) started in")
		);
	}
}
