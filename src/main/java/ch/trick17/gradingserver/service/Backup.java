package ch.trick17.gradingserver.service;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

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
