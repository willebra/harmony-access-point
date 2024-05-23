import {
  AfterViewChecked,
  AfterViewInit,
  ChangeDetectorRef,
  Component,
  OnInit,
  TemplateRef,
  ViewChild
} from '@angular/core';
import {AlertsResult} from './support/alertsresult';
import {HttpClient, HttpParams} from '@angular/common/http';
import {AlertService} from '../common/alert/alert.service';
import {ErrorStateMatcher, ShowOnDirtyErrorStateMatcher} from '@angular/material/core';
import {SecurityService} from '../security/security.service';
import mix from '../common/mixins/mixin.utils';
import BaseListComponent from '../common/mixins/base-list.component';
import FilterableListMixin from '../common/mixins/filterable-list.mixin';
import {ServerSortableListMixin} from '../common/mixins/sortable-list.mixin';
import 'rxjs-compat/add/operator/filter';
import {DialogsService} from '../common/dialogs/dialogs.service';
import ModifiableListMixin from '../common/mixins/modifiable-list.mixin';
import {ServerPageableListMixin} from '../common/mixins/pageable-list.mixin';
import {ApplicationContextService} from '../common/application-context.service';
import {AlertsEntry} from './support/alertsentry';
import {ComponentName} from '../common/component-name-decorator';
import {Moment} from 'moment';
import {DateService} from '../common/customDate/date.service';

@Component({
  templateUrl: 'alerts.component.html',
})
@ComponentName('Alerts')
export class AlertsComponent extends mix(BaseListComponent)
  .with(FilterableListMixin, ServerSortableListMixin, ModifiableListMixin, ServerPageableListMixin)
  implements OnInit, AfterViewInit, AfterViewChecked {

  static readonly ALERTS_URL: string = 'rest/alerts';
  static readonly ALERTS_CSV_URL: string = AlertsComponent.ALERTS_URL + '/csv';
  static readonly ALERTS_TYPES_URL: string = AlertsComponent.ALERTS_URL + '/types';
  static readonly ALERTS_STATUS_URL: string = AlertsComponent.ALERTS_URL + '/status';
  static readonly ALERTS_LEVELS_URL: string = AlertsComponent.ALERTS_URL + '/levels';
  static readonly ALERTS_PARAMS_URL: string = AlertsComponent.ALERTS_URL + '/params';

  TIME_SUFFIX = '_TIME';
  DATE_SUFFIX = '_DATE';
  IMMINENT_SUFFIX = '_IMMINENT';

  @ViewChild('rowProcessed') rowProcessed: TemplateRef<any>;
  @ViewChild('rowWithDateFormatTpl') rowWithDateFormatTpl: TemplateRef<any>;
  @ViewChild('rowWithFutureDateFormatTpl') rowWithFutureDateFormatTpl: TemplateRef<any>;
  @ViewChild('rowWithSpaceAfterCommaTpl') rowWithSpaceAfterCommaTpl: TemplateRef<any>;
  @ViewChild('rowActions') rowActions: TemplateRef<any>;

  aTypes: Array<any>;
  aStatuses: Array<any>;
  aLevels: Array<any>;

  aProcessedValues = ['PROCESSED', 'UNPROCESSED'];

  dynamicFilters: Array<any>;
  dynamicDatesFilter: any;
  nonDateParameters: Array<any>;
  alertTypeWithDate: boolean;

  creationFromMaxDate: Date;
  creationToMinDate: Date;
  creationToMaxDate: Date;

  reportingFromMaxDate: Date;
  reportingToMinDate: Date;
  reportingToMaxDate: Date;

  dynamicDataFromMaxDate: Date;
  dynamicDataToMinDate: Date;
  dynamicDataToMaxDate: Date;

  dateFromName: string;
  dateToName: string;
  displayDomainCheckBox: boolean;
  areRowsDeleted: boolean;
  areRowsEdited: boolean;

  matcher: ErrorStateMatcher = new ShowOnDirtyErrorStateMatcher;

  constructor(private applicationService: ApplicationContextService, private http: HttpClient, private alertService: AlertService,
              private dialogsService: DialogsService, private dateService: DateService,
              private securityService: SecurityService, private changeDetector: ChangeDetectorRef) {
    super();

    this.getAlertTypes();
    this.getAlertLevels();
    this.getAlertStatuses();
  }

  ngOnInit() {
    super.ngOnInit();

    this.dynamicFilters = [];
    this.dynamicDatesFilter = {};
    this.nonDateParameters = [];
    this.alertTypeWithDate = false;

    this.dateFromName = '';
    this.dateToName = '';
    this.displayDomainCheckBox = this.securityService.isCurrentUserSuperAdmin();

    super.filter = {processed: 'UNPROCESSED', domainAlerts: false};

    this.setDateParams();

    super.orderBy = 'creationTime';
    super.asc = false;
    this.areRowsDeleted = false;
    this.areRowsEdited = false;
    this.filterData();
  }

  private setDateParams() {
    let todayEndDay = this.dateService.todayEndDay();

    this.filter.creationTo = todayEndDay;
    console.log('this.filter.creationTo =', this.filter.creationTo)
    this.creationFromMaxDate = todayEndDay;
    this.creationToMinDate = null;
    this.creationToMaxDate = todayEndDay

    this.reportingFromMaxDate = todayEndDay
    this.reportingToMinDate = null;
    this.reportingToMaxDate = todayEndDay

    this.dynamicDataFromMaxDate = todayEndDay
    this.dynamicDataToMinDate = null;
    this.dynamicDataToMaxDate = todayEndDay
  }

  ngAfterViewInit() {
    this.columnPicker.allColumns = [
      {
        name: 'Alert Id',
        width: 200,
        minWidth: 190,
        prop: 'entityId'
      },
      {
        name: 'Processed',
        cellTemplate: this.rowProcessed,
        width: 50,
        minWidth: 40,
      },
      {
        name: 'Alert Type',
        width: 260,
        minWidth: 250,
      },
      {
        name: 'Alert Level',
        width: 100,
        minWidth: 90,
      },
      {
        name: 'Alert Status',
        width: 100,
        minWidth: 90,
      },
      {
        name: 'Creation Time',
        cellTemplate: this.rowWithDateFormatTpl,
        width: 200,
        minWidth: 190,
      },
      {
        name: 'Reporting Time',
        cellTemplate: this.rowWithDateFormatTpl,
        width: 200,
        minWidth: 190,
      },
      {
        name: 'Parameters',
        cellTemplate: this.rowWithSpaceAfterCommaTpl,
        sortable: false,
        width: 200,
        minWidth: 190,
      },
      {
        name: 'Sent Attempts',
        prop: 'attempts',
        width: 50,
        minWidth: 40,
      },
      {
        name: 'Max Attempts',
        width: 50,
        minWidth: 40,
      },
      {
        name: 'Next Attempt',
        cellTemplate: this.rowWithFutureDateFormatTpl,
        width: 200,
        minWidth: 190,
      },
      {
        name: 'Reporting Time Failure',
        cellTemplate: this.rowWithDateFormatTpl,
        width: 200,
        minWidth: 190,
      },
      {
        name: 'Actions',
        cellTemplate: this.rowActions,
        width: 200,
        minWidth: 190,
        canAutoResize: true,
        sortable: false,
        showInitially: true
      }
    ];

    this.columnPicker.selectedColumns = this.columnPicker.allColumns.filter(col => {
      return ['Processed', 'Alert Type', 'Alert Level', 'Alert Status', 'Creation Time', 'Parameters', 'Actions'].indexOf(col.name) != -1
    });

  }

  ngAfterViewChecked() {
    this.changeDetector.detectChanges();
  }

  getAlertTypes(): void {
    this.aTypes = [];
    this.http.get<any[]>(AlertsComponent.ALERTS_TYPES_URL)
      .subscribe(aTypes => this.aTypes = aTypes);
  }

  getAlertStatuses(): void {
    this.aStatuses = [];
    this.http.get<any[]>(AlertsComponent.ALERTS_STATUS_URL)
      .subscribe(aStatuses => this.aStatuses = aStatuses);
  }

  getAlertLevels(): void {
    this.aLevels = [];
    this.http.get<any[]>(AlertsComponent.ALERTS_LEVELS_URL)
      .subscribe(aLevels => this.aLevels = aLevels);
  }

  protected get GETUrl(): string {
    return AlertsComponent.ALERTS_URL;
  }

  public setServerResults(result: AlertsResult) {
    super.count = result.count;
    super.rows = result.alertsEntries;
  }

  protected createAndSetParameters(): HttpParams {
    let filterParams = super.createAndSetParameters();

    if (this.activeFilter.processed) {
      filterParams = filterParams.set('processed', this.activeFilter.processed === 'PROCESSED' ? 'true' : 'false');
    }

    filterParams = this.setDynamicFilterParams(filterParams);

    return filterParams;
  }

  private setDynamicFilterParams(searchParams: HttpParams) {
    if (this.dynamicFilters.length > 0) {
      for (let filter of this.dynamicFilters) {
        searchParams = searchParams.append('parameters', filter || '');
      }
    }

    if (this.alertTypeWithDate) {
      const from = this.dynamicDatesFilter.from;
      if (from) {
        searchParams = searchParams.append('dynamicFrom', from.getTime());
      }

      const to = this.dynamicDatesFilter.to;
      if (to) {
        searchParams = searchParams.append('dynamicTo', to.getTime());
      }
    }
    return searchParams;
  }

  getAlertParameters(alertType: string): Promise<string[]> {
    let searchParams = new HttpParams();
    searchParams = searchParams.append('alertType', alertType);
    return this.http.get<string[]>(AlertsComponent.ALERTS_PARAMS_URL, {params: searchParams}).toPromise();
  }

  async onAlertTypeChanged(alertType: string) {
    this.nonDateParameters = [];
    this.alertTypeWithDate = false;
    this.dynamicFilters = [];
    this.dynamicDatesFilter = [];
    const alertParameters = await this.getAlertParameters(alertType);

    let nonDateParameters = alertParameters.filter((value) => {
      console.log('Value:' + value);
      return (value.search(this.TIME_SUFFIX) === -1 && value.search(this.DATE_SUFFIX) === -1)
    });
    this.nonDateParameters.push(...nonDateParameters);
    let dateParameters = alertParameters.filter((value) => {
      return value.search(this.TIME_SUFFIX) > 0 || value.search(this.DATE_SUFFIX) > 1
    });
    dateParameters.forEach(item => {
      this.dateFromName = item + ' FROM';
      this.dateToName = item + ' TO';
      this.alertTypeWithDate = true;
    });
    this.dynamicDataToMaxDate = this.getDynamicDataToMaxDate(alertType);
    this.dynamicDataFromMaxDate = this.getDynamicDataToMaxDate(alertType);
  }

  private getDynamicDataToMaxDate(alertType: string) {
    return this.isFutureAlert(alertType) ? null : this.dateService.todayEndDay();
  }

  isFutureAlert(alertType: string): boolean {
    return alertType && alertType.includes(this.IMMINENT_SUFFIX);
  }

  onTimestampCreationFromChange(param: Moment) {
    if (param) {
      this.creationToMinDate = param.toDate();
      this.filter.creationFrom = param.toDate();
    } else {
      this.creationToMinDate = null;
      this.filter.creationFrom = null;
    }
  }

  onTimestampCreationToChange(param: Moment) {
    if (param) {
      let date = param.toDate();
      this.dateService.setEndDay(date);
      this.creationFromMaxDate = date;
      this.filter.creationTo = date;
    } else {
      this.creationFromMaxDate = this.dateService.todayEndDay();
      this.filter.creationTo = null;
    }
  }

  onTimestampReportingFromChange(param: Moment) {
    if (param) {
      this.reportingToMinDate = param.toDate();
      this.filter.reportingFrom = param.toDate();
    } else {
      this.reportingToMinDate = null;
      this.filter.reportingFrom = null
    }
  }

  onTimestampReportingToChange(param: Moment) {
    if (param) {
      let date = param.toDate();
      this.dateService.setEndDay(date);
      this.reportingFromMaxDate = date;
      this.filter.reportingTo = date;
    } else {
      this.reportingFromMaxDate = this.dateService.todayEndDay();
      this.filter.reportingTo = null;
    }
  }

  onDynamicDataFromChange(param: Moment) {
    if (param) {
      this.dynamicDataToMinDate = param.toDate();
      this.dynamicDatesFilter.from = param.toDate();
    } else {
      this.dynamicDataToMinDate = null;
      this.dynamicDatesFilter.from = null;
    }
  }

  onDynamicDataToChange(param: Moment) {
    if (param) {
      let date = param.toDate();
      this.dateService.setEndDay(date);
      this.dynamicDataFromMaxDate = date;
      this.dynamicDatesFilter.to = date;
    } else {
      this.dynamicDataFromMaxDate = param.toDate();
      this.dynamicDatesFilter.to = null;
    }
  }

  setIsDirty() {
    super.isChanged = this.areRowsDeleted || this.areRowsEdited;
  }

  delete() {
    this.deleteAlerts(this.selected);
  }

  buttonDeleteAction(row) {
    this.deleteAlerts([row]);
  }

  private deleteAlerts(alerts: AlertsEntry[]) {
    for (const itemToDelete of alerts) {
      itemToDelete.deleted = true;
    }

    super.selected = [];
    this.areRowsDeleted = true;
    this.setIsDirty();
  }

  async doSave(): Promise<any> {
    return this.http.put(AlertsComponent.ALERTS_URL, this.rows).toPromise()
      .then(() => this.loadServerData());
  }

  setProcessedValue(row) {
    this.areRowsEdited = true;
    this.setIsDirty();
  }

  get csvUrl(): string {
    // todo: add dynamic params for csv filtering, if requested
    return AlertsComponent.ALERTS_CSV_URL + '?' + this.createAndSetParameters();
  }

  canDelete() {
    return this.atLeastOneRowSelected() && this.notEveryRowIsDeleted() && !this.isBusy();
  }

  private notEveryRowIsDeleted() {
    return !this.selected.every(el => el.deleted);
  }

  private atLeastOneRowSelected() {
    return this.selected.length > 0;
  }

}
