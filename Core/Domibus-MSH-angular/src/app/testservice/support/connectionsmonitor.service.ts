import {HttpClient, HttpParams} from '@angular/common/http';
import {AlertService} from 'app/common/alert/alert.service';
import {Injectable} from '@angular/core';
import {PartyResponseRo} from '../../party/support/party';
import {PropertiesService, PropertyModel} from '../../properties/support/properties.service';

/**
 * @Author Dussart Thomas
 * @Since 3.3
 */

@Injectable()
export class ConnectionsMonitorService {

  static readonly ALL_PARTIES_URL: string = 'rest/party/list?pageSize=0';
  static readonly TEST_SERVICE_PARTIES_URL: string = 'rest/testservice/parties';
  static readonly TEST_SERVICE_SENDER_URL: string = 'rest/testservice/sender';
  static readonly CONNECTION_MONITOR_URL: string = 'rest/testservice/connectionmonitor';
  static readonly TEST_SERVICE_URL: string = 'rest/testservice';

  constructor(private http: HttpClient, private alertService: AlertService, private propertiesService: PropertiesService) {
  }

  async getMonitors(): Promise<ConnectionMonitorEntry[]> {
    let allParties = await this.http.get<PartyResponseRo[]>(ConnectionsMonitorService.ALL_PARTIES_URL).toPromise();
    if (!allParties || !allParties.length) {
      this.alertService.error('The Pmode is not properly configured.');
      return [];
    }

    let parties = await this.http.get<any[]>(ConnectionsMonitorService.TEST_SERVICE_PARTIES_URL).toPromise();

    if (!parties || !parties.length) {
      const error = 'Could not find testable parties. Self-party could not be an initiator of the test process.';
      this.alertService.error(error);
    }

    let monitors = await this.getMonitorsForParties(parties);
    return allParties.map(party => {
      let cmEntry: ConnectionMonitorEntry = new ConnectionMonitorEntry();
      let allIdentifiers = party.identifiers.sort((id1, id2) => id1.partyId.localeCompare(id2.partyId));
      cmEntry.partyId = allIdentifiers[0].partyId;
      cmEntry.partyName = allIdentifiers.map(id => id.partyId).join('/');

      let monitorKey = Object.keys(monitors).find(k => allIdentifiers.find(id => id.partyId == k));
      if (monitorKey) {
        Object.assign(cmEntry, monitors[monitorKey]);
      } else {
        // cmEntry.status = 'NOT_TESTABLE';
        cmEntry.error = 'Party not testable';
      }
      return cmEntry;
    });
  }

  async getMonitor(partyId: string): Promise<ConnectionMonitorEntry> {
    let monitors = await this.getMonitorsForParties([partyId]);
    console.log('monitors ', monitors);
    return monitors[partyId];
  }

  private getMonitorsForParties(partyIds: string[]): Promise<Map<string, ConnectionMonitorEntry>> {
    if (!partyIds.length) {
      return new Promise<Map<string, ConnectionMonitorEntry>>((resolve, reject) => resolve(new Map()));
    }
    let url = ConnectionsMonitorService.CONNECTION_MONITOR_URL;
    let searchParams = new HttpParams();
    partyIds.forEach(partyId => searchParams = searchParams.append('partyIds', partyId));
    return this.http.get<Map<string, ConnectionMonitorEntry>>(url, {params: searchParams}).toPromise();
  }

  getSenderParty() {
    return this.http.get<string>(ConnectionsMonitorService.TEST_SERVICE_SENDER_URL).toPromise();
  }

  async sendTestMessage(receiverPartyId: string, sender?: string) {
    console.log('sending test message to ', receiverPartyId);

    if (!sender) {
      try {
        sender = await this.getSenderParty();
      } catch (ex) {
        this.alertService.exception('Error getting the sender party:', ex);
        return;
      }
    }
    const payload = {sender: sender, receiver: receiverPartyId};
    return await this.http.post<string>(ConnectionsMonitorService.TEST_SERVICE_URL, payload).toPromise();
  }

  async setMonitorState(partyId: string, enabled: boolean) {
    let propName = 'domibus.monitoring.connection.party.enabled';
    await this.setState(enabled, partyId, propName);
  }

  async setAlertableState(partyId: string, enabled: boolean) {
    let propName = 'domibus.alert.connection.monitoring.parties';
    await this.setState(enabled, partyId, propName);
  }

  private async setState(enabled: boolean, partyId: string, propName: string) {
    let testableParties = await this.http.get<string[]>(ConnectionsMonitorService.TEST_SERVICE_PARTIES_URL).toPromise();
    if (!testableParties || !testableParties.length) {
      throw new Error('The test service is not properly configured.');
    }
    if (enabled && !testableParties.includes(partyId)) {
      throw new Error(partyId + ' is not configured for testing');
    }

    let prop: PropertyModel = await this.propertiesService.getProperty(propName);

    let enabledParties: string[] = prop.value.split(',').map(p => p.trim()).filter(p => p.toLowerCase() != partyId.toLowerCase());
    // remove old parties that are no longer testable:
    enabledParties = enabledParties.filter(p => testableParties.find(tp => tp.toLowerCase() == p.toLowerCase()));

    if (enabled) {
      enabledParties.push(partyId);
    }
    prop.value = enabledParties.join(',');
    await this.propertiesService.updateProperty(prop);
  }

  async setMonitorStateForAll(list: ConnectionMonitorEntry[], enabled: boolean) {
    let propName = 'domibus.monitoring.connection.party.enabled';
    await this.setStateForAll(propName, enabled, list);
  }

  async setAlertableStateForAll(list: ConnectionMonitorEntry[], enabled: boolean) {
    let propName = 'domibus.alert.connection.monitoring.parties';
    await this.setStateForAll(propName, enabled, list);
  }

  private async setStateForAll(propName: string, enabled: boolean, list: ConnectionMonitorEntry[]) {
    let prop: PropertyModel = await this.propertiesService.getProperty(propName);
    if (enabled) {
      prop.value = list.map(el => el.partyId).join(',');
    } else {
      prop.value = '';
    }
    await this.propertiesService.updateProperty(prop);
  }

  async setDeleteHistoryState(partyId: string, enabled: boolean) {
    let propName = 'domibus.monitoring.connection.party.history.delete';
    await this.setState(enabled, partyId, propName);
  }

  async setDeleteHistoryStateForAll(list: ConnectionMonitorEntry[], enabled: boolean) {
    let propName = 'domibus.monitoring.connection.party.history.delete';
    await this.setStateForAll(propName, enabled, list);
  }
}

export class ConnectionMonitorEntry {
  partyId: string;
  partyName?: string;
  testable: boolean;
  monitored: boolean;
  alertable: boolean;
  deleteHistory: boolean;
  status: string;
  lastSent: any;
  lastReceived: any;
  error?: string;
}

