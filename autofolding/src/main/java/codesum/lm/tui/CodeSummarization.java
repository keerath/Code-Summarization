package codesum.lm.tui;

import codesum.lm.main.CodeUtils;
import codesum.lm.main.Settings;
import codesum.lm.topicsum.GibbsSampler;
import codesum.lm.topicsum.TopicSum;
import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParameterException;
import com.google.common.collect.Lists;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.FilenameFilter;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.List;

/**
 * Created by keerathj on 28/10/15.
 */
public class CodeSummarization {

    /**
     * Command line parameters
     */
    public static class Parameters {

        @Parameter(names = {"-w", "--workingDir"}, description = "Working directory where the topic model creates necessary files", required = true)
        String workingDir;

        @Parameter(names = {"-d", "--projectsDir"}, description = "Directory containing project subdirectories", required = true)
        String projectsDir;

        @Parameter(names = {"-i", "--iterations"}, description = "Number of iterations for the topic model")
        int iterations = 1000;

        @Parameter(names = {"-c", "--ratio"}, description = "Desired compression percentage in term of (important file*100/all files)", required = true)
        int compressionRatio;

        @Parameter(names = {"-b", "--backoffTopic"}, description = "Background topic to back off to (0-2)", validateWith = FoldSourceFile.checkBackoffTopic.class)
        int backoffTopic = 2;

        @Parameter(names = {"-t", "--ignoreTests"}, description = "Whether to ignore test classes")
        Boolean ignoreTestFiles = true;
    }

    public static void main(final String[] args) throws Exception {

        final Parameters params = new Parameters();
        final JCommander jc = new JCommander(params);

        try {
            jc.parse(args);
            codeSummarization(params.workingDir, params.projectsDir,
                    params.iterations, params.compressionRatio,
                    params.backoffTopic, params.ignoreTestFiles);
        } catch (final ParameterException e) {
            System.out.println(e.getMessage());
            jc.usage();
        }

    }

    public static void codeSummarization(final String workingDir,
                                         final String projectsDir,
                                         final int iterations,
                                         final int compressionRatio,
                                         final int backOffTopic,
                                         final boolean ignoreTestFiles) throws Exception {

        // Get all projects in projects directory
        final File projDir = new File(projectsDir);
        final String[] projects = projDir.list(new FilenameFilter() {
            @Override
            public boolean accept(final File current, final String name) {
                return new File(current, name).isDirectory();
            }
        });
        int count = 1;
        List<List<String>> projectLists = Lists.partition(Arrays.asList(projects), 20);
        for (List<String> projectsList : projectLists) {
            System.out.println("Batch" + count);
            System.out.println("==========================");
            count ++;
            GibbsSampler gibbsSampler = TrainTopicModel.trainTopicModel(workingDir, projectsDir,
                    projectsList.toArray(new String[projectsList.size()]), iterations);
            for (String projectPath : projectsList) {
                ListSalientFiles.listSalientFiles(projectsDir, projectPath, compressionRatio,
                        backOffTopic, gibbsSampler, ignoreTestFiles);
            }
        }
    }
}
