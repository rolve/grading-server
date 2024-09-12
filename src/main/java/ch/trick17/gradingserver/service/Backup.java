package ch.trick17.gradingserver.service;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class Backup {

    @PersistenceContext
    private EntityManager entityManager;

    @Scheduled(fixedRate = 60 * 60 * 1000, initialDelay = 60 * 60 * 1000)
    @Transactional
    public void backupDatabase() {
        entityManager.createNativeQuery("BACKUP TO 'backup/web-app-backup.zip'").executeUpdate();
    }
}
