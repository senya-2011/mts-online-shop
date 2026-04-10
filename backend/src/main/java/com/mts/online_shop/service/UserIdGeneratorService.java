package com.mts.online_shop.service;

import com.mts.online_shop.repository.UserRepository;
import com.mts.online_shop.security.XmlUserDetailsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.concurrent.atomic.AtomicLong;

@Service
public class UserIdGeneratorService {

    private static final Logger log = LoggerFactory.getLogger(UserIdGeneratorService.class);
    private final UserRepository userRepository;
    private final XmlUserDetailsService xmlUserDetailsService;
    private final AtomicLong nextId;

    public UserIdGeneratorService(UserRepository userRepository, XmlUserDetailsService xmlUserDetailsService) {
        this.userRepository = userRepository;
        this.xmlUserDetailsService = xmlUserDetailsService;
        this.nextId = new AtomicLong(calculateNextId());
        log.info("UserIdGenerator initialized with next ID: {}", nextId.get());
    }

    /**
     * Generate next unique user ID
     * @return next available ID
     */
    public synchronized Long generateNextId() {
        Long id = nextId.getAndIncrement();
        log.debug("Generated user ID: {}", id);
        return id;
    }

    /**
     * Calculate next ID based on existing users in database
     * @return next available ID
     */
    private Long calculateNextId() {
        try {
            // Get max ID from database
            Long maxDbId = userRepository.findMaxUserId();
            if (maxDbId == null) {
                maxDbId = 0L;
            }
            
            // Get max ID from XML users
            Long maxXmlId = getMaxXmlUserId();
            if (maxXmlId == null) {
                maxXmlId = 0L;
            }
            
            // Use the maximum of both sources
            Long maxId = Math.max(maxDbId, maxXmlId);
            Long nextId = maxId + 1;
            log.info("Calculated next user ID: {} (max DB ID: {}, max XML ID: {})", nextId, maxDbId, maxXmlId);
            return nextId;
        } catch (Exception e) {
            log.error("Error calculating next user ID, using default: {}", e.getMessage(), e);
            return 1L;
        }
    }
    
    private Long getMaxXmlUserId() {
        // This method should be implemented in XmlUserDetailsService
        // For now, we'll use a simple approach by checking existing users
        try {
            // Get all users from XML and find max ID
            return xmlUserDetailsService.getMaxUserId();
        } catch (Exception e) {
            log.warn("Error getting max XML user ID: {}", e.getMessage());
            return 0L;
        }
    }

    /**
     * Reset ID generator (for testing)
     */
    public void reset() {
        nextId.set(calculateNextId());
        log.info("UserIdGenerator reset to: {}", nextId.get());
    }
}
