package eu.domibus.core.audit;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import eu.domibus.api.model.MSHRole;
import eu.domibus.api.multitenancy.Domain;
import eu.domibus.api.multitenancy.DomainService;
import eu.domibus.api.multitenancy.DomainTaskExecutor;
import eu.domibus.api.property.DomibusConfigurationService;
import eu.domibus.api.security.AuthUtils;
import eu.domibus.core.audit.envers.ModificationType;
import eu.domibus.core.audit.model.Audit;
import eu.domibus.core.audit.model.MessageAudit;
import eu.domibus.core.audit.model.PModeArchiveAudit;
import eu.domibus.core.audit.model.PModeAudit;
import eu.domibus.core.converter.AuditLogCoreMapper;
import eu.domibus.core.util.AnnotationsUtil;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.*;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.Date;
import java.util.List;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 * @author Thomas Dussart
 * @since 4.0
 */
@RunWith(MockitoJUnitRunner.class)
public class AuditServiceImplTest {

    @Spy
    private AnnotationsUtil annotationsUtil;

    @Mock
    private AuditLogCoreMapper auditLogCoreMapper;

    @Mock
    private AuditDao auditDao;

    @Mock
    private AuthUtils authUtils;

    @Spy
    @InjectMocks
    private AuditServiceImpl auditService;

    @Mock
    private DomainService domainService;

    @Mock
    private DomibusConfigurationService domibusConfigurationService;

    @Mock
    private DomainTaskExecutor domainTaskExecutor;


    @Test
    public void listAuditTarget() {
        when(domibusConfigurationService.isExtAuthProviderEnabled()).thenReturn(false);
        List<String> targets = auditService.listAuditTarget();
        targets.forEach(System.out::println);
        assertEquals(10, targets.size()); // maybe it is better to delete
        assertTrue(targets.contains("User"));
        assertTrue(targets.contains("Pmode"));
        assertTrue(targets.contains("Pmode Archive"));
        assertTrue(targets.contains("Message"));
        assertTrue(targets.contains("Message filter"));
        assertTrue(targets.contains("Jms message"));
        assertTrue(targets.contains("PluginUser"));
        assertTrue(targets.contains("Certificate"));
        assertTrue(targets.contains("Truststore"));
    }

    @Test
    public void listAuditTarget_ExtAuthProvider() {
        when(domibusConfigurationService.isExtAuthProviderEnabled()).thenReturn(true);
        List<String> targets = auditService.listAuditTarget();
        targets.forEach(System.out::println);
        assertEquals(9, targets.size()); // maybe it is better to delete
        assertFalse(targets.contains("User"));
        assertTrue(targets.contains("Pmode"));
        assertTrue(targets.contains("Pmode Archive"));
        assertTrue(targets.contains("Message"));
        assertTrue(targets.contains("Message filter"));
        assertTrue(targets.contains("Jms message"));
        assertTrue(targets.contains("PluginUser"));
        assertTrue(targets.contains("Certificate"));
        assertTrue(targets.contains("Truststore"));
    }

    @Test
    public void listAudit() {

        Audit audit = Mockito.mock(Audit.class);
        List<Audit> audits = Lists.newArrayList(audit);
        Date from = new Date();
        when(auditDao.listAudit(
                Sets.newHashSet("User", "Pmode"),
                Sets.newHashSet("ADD"),
                Sets.newHashSet("Admin"),
                from,
                from,
                0,
                10))
                .thenReturn(audits);
        when(auditDao.listAudit(
                Sets.newHashSet("User", "Pmode"),
                Sets.newHashSet("ADD"),
                Sets.newHashSet("Admin"),
                from,
                from,
                0,
                10))
                .thenReturn(audits);
        auditService.listAudit(
                Sets.newHashSet("User", "Pmode"),
                Sets.newHashSet("ADD"),
                Sets.newHashSet("Admin"),
                from,
                from,
                0,
                10, true);
        verify(auditLogCoreMapper, times(1)).auditLogListToAuditList(audits);

    }

    @Test
    public void countAudit() {
        Date from = new Date();
        when(auditDao.countAudit(
                Sets.newHashSet("User", "Pmode"),
                Sets.newHashSet("ADD"),
                Sets.newHashSet("Admin"),
                from,
                from))
                .thenReturn(1L);
        auditService.countAudit(
                Sets.newHashSet("User", "Pmode"),
                Sets.newHashSet("ADD"),
                Sets.newHashSet("Admin"),
                from,
                from, true);

        verify(auditDao, times(1)).countAudit(
                Sets.newHashSet("User", "Pmode"),
                Sets.newHashSet("ADD"),
                Sets.newHashSet("Admin"),
                from,
                from
        );
    }

    @Test
    public void addMessageResentAudit() {
        when(authUtils.getAuthenticatedUser()).thenReturn("thomas");
        auditService.addMessageResentAudit("resendMessageId");
        ArgumentCaptor<MessageAudit> messageAuditCaptor = ArgumentCaptor.forClass(MessageAudit.class);
        verify(auditDao, times(1)).saveMessageAudit(messageAuditCaptor.capture());
        MessageAudit value = messageAuditCaptor.getValue();
        assertEquals("resendMessageId", value.getId());
        assertEquals("thomas", value.getUserName());
        assertEquals(ModificationType.RESENT, value.getModificationType());
        assertNotNull(value.getRevisionDate());
    }

    @Test
    public void addMessageDownloadedAudit() {
        when(authUtils.getAuthenticatedUser()).thenReturn("thomas");
        auditService.addMessageDownloadedAudit("resendMessageId", MSHRole.RECEIVING);
        ArgumentCaptor<MessageAudit> messageAuditCaptor = ArgumentCaptor.forClass(MessageAudit.class);
        verify(auditDao, times(1)).saveMessageAudit(messageAuditCaptor.capture());
        MessageAudit value = messageAuditCaptor.getValue();
        assertEquals("resendMessageId", value.getId());
        assertEquals("thomas", value.getUserName());
        assertEquals(ModificationType.DOWNLOADED, value.getModificationType());
        assertNotNull(value.getRevisionDate());
    }

    @Test
    public void addPModeDownloadedAudit() {
        when(authUtils.getAuthenticatedUser()).thenReturn("thomas");
        auditService.addPModeDownloadedAudit(1);
        ArgumentCaptor<PModeAudit> messageAuditCaptor = ArgumentCaptor.forClass(PModeAudit.class);
        verify(auditDao, times(1)).savePModeAudit(messageAuditCaptor.capture());
        PModeAudit value = messageAuditCaptor.getValue();
        assertEquals("1", value.getId());
        assertEquals("thomas", value.getUserName());
        assertEquals(ModificationType.DOWNLOADED, value.getModificationType());
        assertNotNull(value.getRevisionDate());
    }

    @Test
    public void addPModeArchiveDownloadedAudit() {
        when(authUtils.getAuthenticatedUser()).thenReturn("admin");
        auditService.addPModeArchiveDownloadedAudit(1);
        ArgumentCaptor<PModeArchiveAudit> messageAuditCaptor = ArgumentCaptor.forClass(PModeArchiveAudit.class);
        verify(auditDao, times(1)).savePModeArchiveAudit(messageAuditCaptor.capture());
        PModeArchiveAudit value = messageAuditCaptor.getValue();
        assertEquals("1", value.getId());
        assertEquals("admin", value.getUserName());
        assertEquals(ModificationType.DOWNLOADED, value.getModificationType());
        assertNotNull(value.getRevisionDate());
    }

    @Test
    public void addJmsMessageDeletedAudit() {
        Domain domain = new Domain();
        domain.setCode("domain1");
        when(domainService.getDomain("domain1")).thenReturn(domain);
        when(domibusConfigurationService.isMultiTenantAware()).thenReturn(true);
        when(authUtils.getAuthenticatedUser()).thenReturn("thomas");

        auditService.addJmsMessageDeletedAudit("resendMessageId", "fromQueue", "domain1");
        ArgumentCaptor<ModificationType> modificationTypeArgumentCaptor = ArgumentCaptor.forClass(ModificationType.class);
        ArgumentCaptor<String> messageIdArgumentCaptor = ArgumentCaptor.forClass(String.class);

        verify(auditService, times(1)).handleSaveJMSMessage(messageIdArgumentCaptor.capture(), anyString(), modificationTypeArgumentCaptor.capture(), anyString());

        ModificationType modificationType = modificationTypeArgumentCaptor.getValue();
        String messageId = messageIdArgumentCaptor.getValue();
        assertEquals("resendMessageId", messageId);
        assertEquals(ModificationType.DEL, modificationType);
    }

    @Test
    public void addJmsMessageMovedAudit() {
        Domain domain = new Domain();
        domain.setCode("domain1");
        when(domainService.getDomain("domain1")).thenReturn(domain);
        when(domibusConfigurationService.isMultiTenantAware()).thenReturn(true);
        when(authUtils.getAuthenticatedUser()).thenReturn("thomas");

        auditService.addJmsMessageMovedAudit("resendMessageId", "fromQueue", "toQueue", "domain1");

        ArgumentCaptor<ModificationType> modificationTypeArgumentCaptor = ArgumentCaptor.forClass(ModificationType.class);
        ArgumentCaptor<String> messageIdArgumentCaptor = ArgumentCaptor.forClass(String.class);
        verify(auditService, times(1)).handleSaveJMSMessage(messageIdArgumentCaptor.capture(), anyString(), modificationTypeArgumentCaptor.capture(), anyString());
        ModificationType modificationType = modificationTypeArgumentCaptor.getValue();
        String messageId = messageIdArgumentCaptor.getValue();
        assertEquals("resendMessageId", messageId);
        assertEquals(ModificationType.MOVED, modificationType);
    }

}
