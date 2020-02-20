package domibus.ui.functional;

import ddsl.dcomponents.DomibusPage;
import ddsl.dcomponents.popups.Dialog;
import ddsl.enums.DMessages;
import ddsl.enums.DRoles;
import ddsl.enums.PAGES;
import pages.jms.JMSSelect;
import utils.BaseTest;
import org.json.JSONObject;
import org.testng.SkipException;
import org.testng.annotations.Test;
import org.testng.asserts.SoftAssert;
import pages.jms.JMSMonitoringPage;
import pages.jms.JMSMoveMessageModal;
import utils.Generator;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

/**
 * @author Catalin Comanici
 * @since 4.1
 */
public class JMSMessPgTest extends BaseTest {

    /* JMS-7 - Delete message */
    @Test(description = "JMS-7", groups = {"multiTenancy", "singleTenancy"})
    public void deleteJMSMessage() throws Exception {
        SoftAssert soft = new SoftAssert();

        login(data.getAdminUser()).getSidebar().goToPage(PAGES.JMS_MONITORING);
        JMSMonitoringPage page = new JMSMonitoringPage(driver);

        int noOfMessages = page.filters().getJmsQueueSelect().selectQueueWithMessages();
        if (noOfMessages > 0) {
            log.info("deleting first message listed");
            HashMap<String, String> rowInfo = page.grid().getRowInfo(0);
            page.grid().selectRow(0);
            page.getDeleteButton().click();
            log.info("cancel delete");
            page.getCancelButton().click();
            new Dialog(driver).confirm();
            soft.assertTrue(page.grid().scrollTo("ID", rowInfo.get("ID")) >= 0, "Message still present in the grid after user cancels delete operation");

            log.info("deleting first message listed");
            HashMap<String, String> rowInfo2 = page.grid().getRowInfo(0);
            page.grid().selectRow(0);
            log.info("click delete");
            page.getDeleteButton().click();
            log.info("saving ");
            page.getSaveButton().click();

            log.info("check message is deleted from grid");
            soft.assertTrue(page.grid().scrollTo("ID", rowInfo2.get("ID")) < 0, "Message NOT present in the grid after delete operation");

        }

        soft.assertAll();
    }

    /*JMS-8 - Move message*/
    @Test(description = "JMS-8", groups = {"multiTenancy", "singleTenancy"})
    public void moveMessage() throws Exception {
        SoftAssert soft = new SoftAssert();
        login(data.getAdminUser()).getSidebar().goToPage(PAGES.JMS_MONITORING);
        JMSMonitoringPage page = new JMSMonitoringPage(driver);

        log.info("checking no of messages");
        page.grid().waitForRowsToLoad();
        int noOfMessInDQL = page.grid().getPagination().getTotalItems();

        int noOfMessages = page.filters().getJmsQueueSelect().selectQueueWithMessagesNotDLQ();
        page.grid().waitForRowsToLoad();

        String queuename = page.filters().getJmsQueueSelect().getSelectedValue();

        if (noOfMessages > 0) {
            log.info("moving the first message");
            page.grid().selectRow(0);
            page.getMoveButton().click();

            log.info("canceling");
            JMSMoveMessageModal modal = new JMSMoveMessageModal(driver);
            modal.getQueueSelect().selectDLQQueue();
            modal.clickCancel();

            log.info("checking the number of messages");
            soft.assertEquals(noOfMessages, page.grid().getPagination().getTotalItems(), "Number of messages in current queue is not changed");

            page.filters().getJmsQueueSelect().selectDLQQueue();
            page.grid().waitForRowsToLoad();

            log.info("getting no of messages in DLQ queue");
            soft.assertEquals(noOfMessInDQL, page.grid().getPagination().getTotalItems(), "Number of messages in DLQ message queue is not changed");

            log.info("selecting queue " + queuename);
            page.filters().getJmsQueueSelect().selectOptionByText(queuename);
            page.grid().waitForRowsToLoad();

            log.info("getting info on row 0");
            HashMap<String, String> rowInfo = page.grid().getRowInfo(0);
            page.grid().selectRow(0);
            log.info("moving message on row 0 to DLQ queue");
            page.getMoveButton().click();

            modal.getQueueSelect().selectDLQQueue();
            modal.clickOK();

            page.grid().waitForRowsToLoad();

            log.info("checking success message");
            soft.assertTrue(!page.getAlertArea().isError(), "Success message is shown");
            soft.assertEquals(page.getAlertArea().getAlertMessage(), DMessages.JMS_MOVE_MESSAGE_SUCCESS, "Correct message is shown");

            log.info("checking number of listed messages for this queue");
            soft.assertTrue(page.grid().getPagination().getTotalItems() == noOfMessages - 1, "Queue has one less message");

            log.info("selecting DLQ queue");
            page.filters().getJmsQueueSelect().selectDLQQueue();
            page.grid().waitForRowsToLoad();

            log.info("checking no of messages in DLQ queue");
            soft.assertEquals(noOfMessInDQL + 1, page.grid().getPagination().getTotalItems(), "DQL queue has one more message after the move");

            int index = page.grid().scrollTo("ID", rowInfo.get("ID"));
            log.info("checking the moved message is present in the grid");
            soft.assertTrue(index > -1, "DQL queue contains the new message");
        } else {
            throw new SkipException("Not enough messages in any of the queues to run test");
        }

        soft.assertAll();
    }

    /*JMS-9 - Domain admin logs in and views messages*/
    @Test(description = "JMS-9", groups = {"multiTenancy"})
    public void adminOpenJMSMessagesPage() throws Exception {
        SoftAssert soft = new SoftAssert();
        String domain = getNonDefaultDomain();
        log.info("checking for domain " + domain);
        JSONObject user = getUser(domain, DRoles.ADMIN, true, false, false);

        login(user.getString("userName"), data.defaultPass());
        log.info("logging in with admin " + user.getString("userName"));

        JMSMonitoringPage page = new JMSMonitoringPage(driver);
        page.getSidebar().goToPage(PAGES.JMS_MONITORING);

        log.info("checking domain name in the title");
        soft.assertEquals(page.getDomainFromTitle(), domain, "Page title shows correct domain");
        soft.assertTrue(page.filters().isLoaded(), "Filters are loaded and visible");

        List<String> sources = page.filters().getJmsQueueSelect().getOptionsTexts();
        log.info("checking message numbers are missing from queue source names");
        for (String source : sources) {
            soft.assertTrue(!source.matches("\\(\\d\\)"), "Message numbers are not shown when admin is logged in");
        }

        List<HashMap<String, String>> allInfo = page.grid().getAllRowInfo();
        log.info("checking messages contain domain name in Custom prop field");
        for (HashMap<String, String> info : allInfo) {
            soft.assertTrue(info.get("Custom prop").contains(domain));
        }

        soft.assertAll();
    }

    @Test(description = "JMS-10", groups = {"multiTenancy"})
    public void changeDomainAfterSelection() throws Exception {
        SoftAssert soft = new SoftAssert();
        log.info("Login into application and navigate to JMS Monitoring page");
        login(data.getAdminUser()).getSidebar().goToPage(PAGES.JMS_MONITORING);

        JMSMonitoringPage jmsPage = new JMSMonitoringPage(driver);

        log.info("Extract current domain name from page title");
        String currentDomain = jmsPage.getDomainFromTitle();
        log.info("select any message queue having some messages");
        jmsPage.filters().getJmsQueueSelect().selectQueueWithMessages();

        log.info("wait for grid row to load");
        jmsPage.grid().waitForRowsToLoad();

        log.info("select first row");
        jmsPage.grid().selectRow(0);

        log.info("Confirm status of Move button and Delete button");
        soft.assertTrue(jmsPage.moveButton.isEnabled(), "Move button is enabled on row selection");
        soft.assertTrue(jmsPage.deleteButton.isEnabled(), "Delete button is enabled on row selection");

        log.info("select other domain from domain selector");
        jmsPage.getDomainSelector().selectOptionByIndex(1);
        log.info("Wait for page title");
        jmsPage.waitForTitle();
        log.info("Wait for grid row to load");
        jmsPage.grid().waitForRowsToLoad();

        log.info("Compare old and new domain name");
        soft.assertTrue(!jmsPage.getDomainFromTitle().equals(currentDomain), "Current domain differs from old domain");

        log.info("Check status of move button and delete button");
        soft.assertFalse(jmsPage.moveButton.isEnabled(), "Move button is not enabled");
        soft.assertFalse(jmsPage.deleteButton.isEnabled(), "Delete button is not enabled");
        soft.assertAll();
    }

    @Test(description = "JMS-11", groups = {"multiTenancy"})
    public void changeDomainFromSecondPage() throws Exception {
        SoftAssert soft = new SoftAssert();
        log.info("Login into application and navigate to JMS Monitoring page");
        login(data.getAdminUser()).getSidebar().goToPage(PAGES.JMS_MONITORING);

        JMSMonitoringPage jmsPage = new JMSMonitoringPage(driver);
        log.info("Extract current domain name from page title");
        String currentDomain = jmsPage.getDomainFromTitle();

        log.info("select any message queue having some messages");
        int noOfMsgs = jmsPage.filters().getJmsQueueSelect().selectQueueWithMessagesNotDLQ();
        if (noOfMsgs > 10) {
            jmsPage.grid().getPagination().goToPage(2);
        }
        soft.assertTrue(jmsPage.grid().getPagination().getActivePage() == 2, "Selected page is 2");
        log.info("Change domain");
        jmsPage.getDomainSelector().selectOptionByIndex(1);

        log.info("Wait for grid row to load");
        jmsPage.grid().waitForRowsToLoad();

        log.info("Check active page is different from 2 i.e earlier selected one");
        soft.assertTrue(jmsPage.grid().getPagination().getActivePage() != 2, "Active page number in not 2");

        log.info("Check current number of messages");
        int currentNoOfMsgs = jmsPage.grid().getPagination().getTotalItems();

        log.info("Total number of message count before and after domain change is different");
        soft.assertTrue(currentNoOfMsgs != noOfMsgs, " Total number of messages are different");
        soft.assertAll();

    }

    @Test(description = "JMS-21", groups = {"multiTenancy", "singleTenancy"})
    public void checkReceivedUpTo() throws Exception {
        SoftAssert soft = new SoftAssert();
        log.info("Login into application and navigate to JMS Monitoring page");
        login(data.getAdminUser()).getSidebar().goToPage(PAGES.JMS_MONITORING);

        JMSMonitoringPage jmsPage = new JMSMonitoringPage(driver);
        jmsPage.waitForTitle();
        DateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy");

        log.info("Find current system date");
        String currentDate = dateFormat.format(new Date());
        log.info("Current date is :" + currentDate);
        if (data.isIsMultiDomain()) {
            do {
                log.info("Current domain is" + jmsPage.getDomainSelector().getSelectedValue());


                log.info("Extract Received Upto field default data");
                String defaultDate = jmsPage.receivedDateField.get(1).getAttribute("value");

                log.info("Default value of Received upto field : " + defaultDate);

                String defaultReceivedUpto[] = defaultDate.split(" ");

                log.info(defaultReceivedUpto[0] + " : Date from Received upto field for domain " + jmsPage.getDomainSelector().getSelectedValue());
                log.info(defaultReceivedUpto[1] + " : Time from Received upto field for domain " + jmsPage.getDomainSelector().getSelectedValue());

                soft.assertTrue(defaultReceivedUpto[0].equals(currentDate), "Dates are same");
                soft.assertTrue(defaultReceivedUpto[1].equals("23:59"), "Time matches 23:59");
                if (jmsPage.getDomainSelector().getSelectedValue().equals("domain1")) {
                    break;
                }

                if (data.isIsMultiDomain()) {
                    log.info("Change domain");
                    jmsPage.getDomainSelector().selectOptionByIndex(1);
                }
            } while (jmsPage.getDomainFromTitle().equals("domain1"));
        } else {

            log.info("Extract Received Upto field default data");
            String defaultDate = jmsPage.receivedDateField.get(1).getAttribute("value");

            log.info("Default value of Received upto field : " + defaultDate);

            String defaultReceivedUpto[] = defaultDate.split(" ");

            log.info(defaultReceivedUpto[0] + " : Date from Received upto field for domain ");
            log.info(defaultReceivedUpto[1] + " : Time from Received upto field for domain ");

            soft.assertTrue(defaultReceivedUpto[0].equals(currentDate), "Dates are same");
            soft.assertTrue(defaultReceivedUpto[1].equals("23:59"), "Time matches 23:59");

        }
        soft.assertAll();

    }

    @Test(description = "JMS-12", groups = {"multiTenancy"})
    public void jmsMsgDelOnDomainChange() throws Exception {
        SoftAssert soft = new SoftAssert();
        log.info("Login into application and navigate to JMS Monitoring page");
        login(data.getAdminUser()).getSidebar().goToPage(PAGES.JMS_MONITORING);

        JMSMonitoringPage jmsPage = new JMSMonitoringPage(driver);
        log.info("Extract current domain name from page title");
        String currentDomain = jmsPage.getDomainFromTitle();

        log.info("select any message queue having some messages");
        int noOfMsgs = jmsPage.filters().getJmsQueueSelect().selectQueueWithMessagesNotDLQ();
        log.info("Message count from selected queue other than DLQ : " + noOfMsgs);

        soft.assertTrue(noOfMsgs != 0, "JMS queue has some messages");
        log.info("Select first row");
        jmsPage.grid().selectRow(0);

        log.info("Verify status of Move button and Delete button");
        soft.assertTrue(jmsPage.getMoveButton().isEnabled(), "Move button is enabled on selection");
        soft.assertTrue(jmsPage.getDeleteButton().isEnabled(), "Delete button is enabled on selection");

        log.info("Change domain ");
        jmsPage.getDomainSelector().selectOptionByIndex(1);
        jmsPage.waitForTitle();

        log.info("Check message count in queue");

        if (jmsPage.grid().getPagination().getTotalItems() == 0) {
            log.info("Select any other jms queue if default queue DLQ has no message");
            jmsPage.filters().getJmsQueueSelect().selectQueueWithMessages();
            jmsPage.grid().waitForRowsToLoad();
        }

        int totalCount = jmsPage.grid().getPagination().getTotalItems();
        log.info("Current message count is " + totalCount);

        log.info("Select first row");
        jmsPage.grid().selectRow(0);

        log.info("Click on delete button");
        jmsPage.getDeleteButton().click();

        log.info("Click on save button");
        jmsPage.getSaveButton().click();

        log.info("Check presence of success message on deletion");
        soft.assertTrue(jmsPage.getAlertArea().getAlertMessage().contains("success"), "Success message is shown on deletion");

        log.info("Verify queue message count as 1 less than before");
        soft.assertTrue(jmsPage.grid().getPagination().getTotalItems() == totalCount - 1, "Queue message count is 1 less");
        soft.assertAll();

    }

    @Test(description = "JMS-23", groups = {"multiTenancy", "singleTenancy"})
    public void queueMsgCountOnInputFilter() throws Exception {
        SoftAssert soft = new SoftAssert();

        JMSMonitoringPage jmsPage = new JMSMonitoringPage(driver);

        if (data.isIsMultiDomain()) {
            log.info("Create Admin user for default domain");
            String user = Generator.randomAlphaNumeric(10);
            rest.createUser(user, DRoles.ADMIN, data.defaultPass(), "default");

            log.info("Login into application with Admin user of Multitenancy and navigate to JMS Monitoring page");
            login(user, data.defaultPass());

            DomibusPage page = new DomibusPage(driver);
            do {
                log.info("Navigate to Jms Monitoring Page");
                page.getSidebar().goToPage(PAGES.JMS_MONITORING);
                jmsPage.waitForTitle();

                log.info("Wait for grid row to load");
                jmsPage.grid().waitForRowsToLoad();

                log.info("Queue name is : " + jmsPage.multiSelectInputField.get(0).getText());

                log.info("check if no count is shown for admin user of :" + jmsPage.getDomainFromTitle());
                log.info("Count shown in queue name :" + jmsPage.getCountFromQueueName(jmsPage.multiSelectInputField.get(0).getText()));

                soft.assertTrue(jmsPage.getCountFromQueueName(jmsPage.multiSelectInputField.get(0).getText()) == null, "Message count is not shown for Admin user for Multi Tenancy");
                log.info("Break from loop if current domain name is domain1");
                if (jmsPage.getDomainFromTitle().equals("domain1")) {
                    break;
                }
                log.info("Logout from application");
                logout();

                log.info("Create new user for domain1");
                String userr = Generator.randomAlphaNumeric(10);
                rest.createUser(userr, DRoles.ADMIN, data.defaultPass(), "domain1");

                log.info("Login into application with admin user of domain1");
                login(userr, data.defaultPass());

            } while (jmsPage.getDomainFromTitle().equals("domain1"));

        } else {

            log.info("Login into application with Admin user of Single tenancy and navigate to JMS Monitoring page");
            login(data.getAdminUser()).getSidebar().goToPage(PAGES.JMS_MONITORING);

            jmsPage.waitForTitle();
            log.info("Wait for grid row to load");
            jmsPage.grid().waitForRowsToLoad();

            log.info("Queue name is : " + jmsPage.multiSelectInputField.get(0).getText());
            soft.assertTrue(jmsPage.getCountFromQueueName(jmsPage.multiSelectInputField.get(0).getText()) != null, "Message count is  shown for Admin user for Single Tenancy");
            log.info("Count shown in queue name " + jmsPage.getCountFromQueueName(jmsPage.multiSelectInputField.get(0).getText()));
        }
        soft.assertAll();
    }

    @Test(description = "JMS-24", groups = {"multiTenancy", "singleTenancy"})
    public void queueMsgCountOnMovePopUp() throws Exception {
        SoftAssert soft = new SoftAssert();

        JMSMonitoringPage jmsPage = new JMSMonitoringPage(driver);
        if (data.isIsMultiDomain()) {

            log.info("Create Admin user for default domain");
            String user = Generator.randomAlphaNumeric(10);
            rest.createUser(user, DRoles.ADMIN, data.defaultPass(), "default");

            log.info("Login into application with Admin user of Multitenancy and navigate to JMS Monitoring page");
            login(user, data.defaultPass());
            log.info("uploading pmode");
            rest.uploadPMode("pmodes/Edelivery-blue-lessRetryTimeout.xml", null);
            String pluginUser = Generator.randomAlphaNumeric(10);

            log.info("Create plugin user");
            rest.createPluginUser(pluginUser, DRoles.ADMIN, data.defaultPass(), null);

            log.info("send message ");
            messageSender.sendMessage(pluginUser, data.defaultPass(), null, null);
            do {
                jmsPage.getSidebar().goToPage(PAGES.JMS_MONITORING);
                jmsPage.waitForTitle();

                log.info("Select queue [internal] domibus.notification.webservice ");
                jmsPage.filters().getJmsQueueSelect().selectOptionByText("[internal] domibus.notification.webservice");
                log.info("Wait for grid row to load");
                jmsPage.grid().waitForRowsToLoad();

                log.info("select first row");
                jmsPage.grid().selectRow(0);

                log.info("Check status of Move button");
                soft.assertTrue(jmsPage.getMoveButton().isEnabled(), "Move button is enabled");

                log.info("Click on Move button");
                jmsPage.getMoveButton().click();
                JMSMoveMessageModal jmsModel = new JMSMoveMessageModal(driver);

                log.info("Click on arrow to open destination fields on Move pop up");
                jmsModel.destinationArrows.get(2).click();

                log.info("Select first queue");
                jmsModel.getQueueSelect().selectOptionByIndex(0);

                log.info("Extract name of selected queue name");
                String selectedQueuee = jmsModel.getQueueSelect().getSelectedValue();

                log.info("Verify presence of no count in selected queue name");
                soft.assertTrue(jmsPage.getCountFromQueueName(selectedQueuee) == null, "Count is not shown for queue on Move pop up for Multi tenant Admin user");

                jmsPage.refreshPage();
                jmsPage.grid().waitForRowsToLoad();

                log.info("Break from loop if current domain name is domain1");
                if (jmsPage.getDomainFromTitle().equals("domain1")) {
                    break;
                }

                log.info("Logout from application");
                logout();
                log.info("Create Admin user for domain1 ");
                String userr = Generator.randomAlphaNumeric(10);
                rest.createUser(userr, DRoles.ADMIN, data.defaultPass(), "domain1");

                log.info("Login into application with Admin user of domain1 and navigate to JMS Monitoring page");
                login(userr, data.defaultPass());

            } while (jmsPage.getDomainFromTitle().equals("domain1"));

        } else {

            log.info("Login into application with Admin user of Single tenancy and navigate to JMS Monitoring page");
            login(data.getAdminUser()).getSidebar().goToPage(PAGES.JMS_MONITORING);

            jmsPage.waitForTitle();
            log.info("Select first grid row if total count >0");
            if (jmsPage.grid().getPagination().getTotalItems() > 0) {
                jmsPage.grid().selectRow(0);

            } else {
                rest.uploadPMode("pmodes/Edelivery-blue-lessRetryTimeout.xml", null);
                String pluginUser = Generator.randomAlphaNumeric(10);

                log.info("Create plugin user");
                rest.createPluginUser(pluginUser, DRoles.ADMIN, data.defaultPass(), null);

                log.info("send message ");
                messageSender.sendMessage(pluginUser, data.defaultPass(), null, null);

                log.info("Select queue [internal] domibus.notification.webservice and Wait for grid row to load");
                jmsPage.filters().getJmsQueueSelect().selectOptionByText("[internal] domibus.notification.webservice");
                jmsPage.grid().waitForRowsToLoad();

                log.info("Select first row");
                jmsPage.grid().selectRow(0);
            }

            soft.assertTrue(jmsPage.getMoveButton().isEnabled(), "Move button is enabled");

            log.info("Click on Move button");
            jmsPage.getMoveButton().click();
            JMSMoveMessageModal jmsModell = new JMSMoveMessageModal(driver);

            log.info("Get selected queue name and verify absence of count number");
            String selectedQueue = jmsModell.getQueueSelect().getSelectedValue();

            log.info("Count shown in queue is : " + jmsPage.getCountFromQueueName(selectedQueue));
            soft.assertTrue(jmsPage.getCountFromQueueName(selectedQueue) != null, "Count is shown for queue on Move pop up for Single tenant Admin user");
        }

        soft.assertAll();


    }

    @Test(description = "JMS-25", groups = {"multiTenancy"})
    public void queueMsgCountForSuperAdmin() throws Exception {
        SoftAssert soft = new SoftAssert();
        log.info("Login into application and navigate to Jms Monitoring Page");

        login(data.getAdminUser()).getSidebar().goToPage(PAGES.JMS_MONITORING);
        JMSMonitoringPage jmsPage = new JMSMonitoringPage(driver);
        do {
            jmsPage.waitForTitle();

            log.info("Wait for grid row to load");
            jmsPage.grid().waitForRowsToLoad();

            log.info("Queue name is : " + jmsPage.multiSelectInputField.get(1).getText());

            log.info("check if count is shown for Super admin user for domain :" + jmsPage.getDomainFromTitle());
            soft.assertTrue(jmsPage.getCountFromQueueName(jmsPage.multiSelectInputField.get(1).getText()) != null, "Message count is not shown for Admin user for Multi Tenancy");
            log.info("Count shown in queue name is : " + jmsPage.getCountFromQueueName(jmsPage.multiSelectInputField.get(1).getText()));

            log.info("Get domain name from page title");
            if (jmsPage.getDomainFromTitle().equals("domain1")) {
                break;
            }
            log.info("Change domain");
            jmsPage.getDomainSelector().selectOptionByIndex(1);

        } while (jmsPage.getDomainFromTitle().equals("domain1"));


        soft.assertAll();
    }


    @Test(description = "JMS-26", groups = {"multiTenancy"})
    public void msgCountOnMoveForSuperAdmin() throws Exception {
        SoftAssert soft = new SoftAssert();

        log.info("Login into application and navigate to Jms Monitoring Page");
        login(data.getAdminUser()).getSidebar().goToPage(PAGES.JMS_MONITORING);

        JMSMonitoringPage jmsPage = new JMSMonitoringPage(driver);

        do {
            jmsPage.waitForTitle();
            log.info("Wait for grid row to load");
            jmsPage.grid().waitForRowsToLoad();

            if (jmsPage.grid().getPagination().getTotalItems() > 0) {
                log.info("select first row if grid row count >0");

                jmsPage.grid().selectRow(0);
            } else {

                log.info("select queue with some messages");
                jmsPage.filters().getJmsQueueSelect().selectQueueWithMessages();
                jmsPage.grid().waitForRowsToLoad();

                log.info("Select first row");
                jmsPage.grid().selectRow(0);

            }
            soft.assertTrue(jmsPage.getMoveButton().isEnabled(), "Move button is enabled");

            log.info("Click on Move button");
            jmsPage.getMoveButton().click();
            JMSMoveMessageModal jmsModel = new JMSMoveMessageModal(driver);

            log.info("Open destination drop down");
            jmsModel.destinationArrows.get(3).click();

            log.info("Select first queue");
            jmsModel.getQueueSelect().selectOptionByIndex(0);

            log.info("Verify presence of count from selected queue name");
            String selectedQueuee = jmsModel.getQueueSelect().getSelectedValue();
            log.info("Message count shown in queue name :" + jmsPage.getCountFromQueueName(selectedQueuee));

            soft.assertTrue(jmsPage.getCountFromQueueName(selectedQueuee) != null, "Count is  shown for queue on Move pop up for Multi tenant Super Admin user");
            jmsPage.refreshPage();
            jmsPage.grid().waitForRowsToLoad();

            log.info("break from loop if domain name is domain1");
            if (jmsPage.getDomainFromTitle().equals("domain1")) {
                break;
            }

            log.info("Change domain");
            jmsPage.getDomainSelector().selectOptionByIndex(1);
        } while (jmsPage.getDomainFromTitle().equals("domain1"));
        soft.assertAll();

    }
}




