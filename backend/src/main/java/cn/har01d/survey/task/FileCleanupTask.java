package cn.har01d.survey.task;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import cn.har01d.survey.repository.AnswerRepository;
import cn.har01d.survey.service.FileService;

@Component
public class FileCleanupTask {

    private static final Logger log = LoggerFactory.getLogger(FileCleanupTask.class);

    private final FileService fileService;
    private final AnswerRepository answerRepository;

    public FileCleanupTask(FileService fileService, AnswerRepository answerRepository) {
        this.fileService = fileService;
        this.answerRepository = answerRepository;
    }

    @Scheduled(cron = "0 0 3 * * *")
    public void cleanupOrphanedFiles() {
        log.info("Starting orphaned file cleanup...");

        List<String> fileUrls = answerRepository.findAllFileUrls();
        Set<String> referencedFileNames = new HashSet<>();
        for (String url : fileUrls) {
            if (url.startsWith("/api/files/")) {
                referencedFileNames.add(url.substring("/api/files/".length()));
            }
        }

        List<String> oldFiles = fileService.listOldFiles(24);
        int deleted = 0;
        for (String fileName : oldFiles) {
            if (!referencedFileNames.contains(fileName)) {
                try {
                    fileService.delete(fileName);
                    deleted++;
                } catch (Exception e) {
                    log.warn("Failed to delete orphaned file: {}", fileName, e);
                }
            }
        }

        log.info("Orphaned file cleanup finished. Deleted {} files.", deleted);
    }
}
