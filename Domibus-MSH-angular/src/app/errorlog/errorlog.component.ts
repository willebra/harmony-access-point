﻿import {ChangeDetectorRef, Component, ElementRef, OnInit, Renderer2, TemplateRef, ViewChild} from '@angular/core';
import {HttpClient, HttpParams} from '@angular/common/http';
import {ErrorLogResult} from './errorlogresult';
import {AlertService} from '../common/alert/alert.service';
import {ErrorlogDetailsComponent} from 'app/errorlog/errorlog-details/errorlog-details.component';
import {MatDialog, MatDialogRef} from '@angular/material';
import mix from '../common/mixins/mixin.utils';
import BaseListComponent from '../common/mixins/base-list.component';
import FilterableListMixin from '../common/mixins/filterable-list.mixin';
import {ServerSortableListMixin} from '../common/mixins/sortable-list.mixin';
import {ServerPageableListMixin} from '../common/mixins/pageable-list.mixin';

@Component({
  moduleId: module.id,
  templateUrl: 'errorlog.component.html',
  providers: [],
  styleUrls: ['./errorlog.component.css']
})

export class ErrorLogComponent extends mix(BaseListComponent)
  .with(FilterableListMixin, ServerSortableListMixin, ServerPageableListMixin)
  implements OnInit {

  dateFormat: String = 'yyyy-MM-dd HH:mm:ssZ';

  @ViewChild('rowWithDateFormatTpl', {static: false}) rowWithDateFormatTpl: TemplateRef<any>;

  timestampFromMaxDate: Date = new Date();
  timestampToMinDate: Date = null;
  timestampToMaxDate: Date = new Date();

  notifiedFromMaxDate: Date = new Date();
  notifiedToMinDate: Date = null;
  notifiedToMaxDate: Date = new Date();

  mshRoles: string[];
  errorCodes: string[];

  static readonly ERROR_LOG_URL: string = 'rest/errorlogs';
  static readonly ERROR_LOG_CSV_URL: string = ErrorLogComponent.ERROR_LOG_URL + '/csv?';

  constructor(private elementRef: ElementRef, private http: HttpClient, private alertService: AlertService,
              public dialog: MatDialog, private changeDetector: ChangeDetectorRef) {
    super();
  }

  ngOnInit() {
    super.ngOnInit();

    super.orderBy = 'timestamp';
    super.asc = false;

    this.filterData();
  }

  ngAfterViewInit() {
    this.columnPicker.allColumns = [
      {
        name: 'Signal Message Id',
        prop: 'errorSignalMessageId'
      },
      {
        name: 'AP Role',
        prop: 'mshRole',
        width: 50
      },
      {
        name: 'Message Id',
        prop: 'messageInErrorId',
      },
      {
        name: 'Error Code',
        // width: 50
      },
      {
        name: 'Error Detail',
        width: 350
      },
      {
        cellTemplate: this.rowWithDateFormatTpl,
        name: 'Timestamp',
        // width: 180
      },
      {
        cellTemplate: this.rowWithDateFormatTpl,
        name: 'Notified',
        width: 50
      }

    ];

    this.columnPicker.selectedColumns = this.columnPicker.allColumns.filter(col => {
      return ['Message Id', 'Error Code', 'Timestamp'].indexOf(col.name) != -1
    });
  }

  ngAfterViewChecked() {
    this.changeDetector.detectChanges();
  }

  protected get name(): string {
    return 'Error Logs';
  }

  protected get GETUrl(): string {
    return ErrorLogComponent.ERROR_LOG_URL;
  }

  setServerResults(result: ErrorLogResult) {
    super.count = result.count;
    super.rows = result.errorLogEntries;

    if (result.filter.timestampFrom) {
      result.filter.timestampFrom = new Date(result.filter.timestampFrom);
    }
    if (result.filter.timestampTo) {
      result.filter.timestampTo = new Date(result.filter.timestampTo);
    }
    if (result.filter.notifiedFrom) {
      result.filter.notifiedFrom = new Date(result.filter.notifiedFrom);
    }
    if (result.filter.notifiedTo) {
      result.filter.notifiedTo = new Date(result.filter.notifiedTo);
    }

    super.filter = result.filter;
    this.mshRoles = result.mshRoles;
    this.errorCodes = result.errorCodes;
  }

  onTimestampFromChange(event) {
    this.timestampToMinDate = event.value;
  }

  onTimestampToChange(event) {
    this.timestampFromMaxDate = event.value;
  }

  onNotifiedFromChange(event) {
    this.notifiedToMinDate = event.value;
  }

  onNotifiedToChange(event) {
    this.notifiedFromMaxDate = event.value;
  }

  showDetails(selectedRow: any) {
    let dialogRef: MatDialogRef<ErrorlogDetailsComponent> = this.dialog.open(ErrorlogDetailsComponent);
    dialogRef.componentInstance.message = selectedRow;
  }

  get csvUrl(): string {
    return ErrorLogComponent.ERROR_LOG_CSV_URL + this.createAndSetParameters();
  }
}
