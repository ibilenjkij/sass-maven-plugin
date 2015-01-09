/*
 * Copyright 2015 Mark Prins, GeoDienstenCentrum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package nl.geodienstencentrum.maven.plugin.sass.compiler;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.testing.MojoRule;
import org.apache.maven.plugin.testing.resources.TestResources;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;

/**
 * Testcase for
 * {@link nl.geodienstencentrum.maven.plugin.sass.compiler.WatchMojo }.
 *
 * @author Mark C. Prins
 */
public class WatchMojoTest {

	/**
	 * Test resources.
	 */
	@Rule
	public TestResources resources = new TestResources();

	/**
	 * test rule.
	 */
	@Rule
	public MojoRule rule = new MojoRule();

	/**
	 * Test method for
	 * {@link nl.geodienstencentrum.maven.plugin.sass.compiler.WatchMojo#execute() }
	 * .
	 *
	 * @throws Exception if any
	 * @see nl.geodienstencentrum.maven.plugin.sass.compiler.WatchMojo#execute()
	 */
	@Test
	public void testExecute() throws Exception {

		// setup mojo and start execution
		final File projectCopy = this.resources
				.getBasedir("maven-compass-test");
		final File pom = new File(projectCopy, "pom.xml");
		assertNotNull("POM file should not be null.", pom);
		assertTrue("POM file should exist as file.",
				pom.exists() && pom.isFile());

		final WatchMojo myMojo = (WatchMojo) this.rule
				.lookupConfiguredMojo(projectCopy, "watch");
		assertNotNull("the 'watch' mojo should exist", myMojo);
		// start 'watch'ing
		synchronized (this) {
			new Thread("sassWatcher") {
				@Override
				public void run() {
					try {
						myMojo.execute();
					} catch (MojoExecutionException | MojoFailureException e) {
						org.junit.Assert.fail("Execution failed: " + e);
						this.interrupt();
					}
				}
			}.start();
			// wait for watcher to start up...
			this.wait(5000);
			// modify a file in the project
			TestResources.touch(new File(projectCopy.getAbsolutePath() + "/src/main/sass/"),
					"_colours.scss");
			// wait for watcher to catch up...
			this.wait(5000);
			// modify another file in the project
			TestResources.cp(new File(projectCopy.getAbsolutePath() + "/src/main/sass/"),
					"compiled.scss", "print.scss");
			// wait for watcher to catch up...
			this.wait(5000);

			// done; lets check compilation results
			TestResources.assertDirectoryContents(
					new File(projectCopy.getAbsolutePath() + "/target/maven-compass-test-1.0/css/"),
					"compiled.css.map", "compiled.css", "print.css.map", "print.css");
			// this may fail when line endings differ, eg. on Windows
			// set up git to check out with native file endings
			TestResources.assertFileContents(projectCopy, "expected.css",
					"target/maven-compass-test-1.0/css/compiled.css");
			TestResources.assertFileContents(projectCopy, "print.css",
					"target/maven-compass-test-1.0/css/print.css");
		}
	}
}