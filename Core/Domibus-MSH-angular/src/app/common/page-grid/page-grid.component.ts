import {AfterViewChecked, ChangeDetectorRef, Component, Input, ViewChild} from '@angular/core';
import {IPageableList, PaginationType} from '../mixins/ipageable-list';
import BaseListComponent from '../mixins/base-list.component';
import {ISortableList} from '../mixins/isortable-list';
import {instanceOfPageableList, instanceOfSortableList} from '../mixins/type.utils';
import {DatatableComponent, SelectionType} from '@swimlane/ngx-datatable';

@Component({
  selector: 'page-grid',
  templateUrl: './page-grid.component.html',
  styleUrls: ['./page-grid.component.css']
})

export class PageGridComponent {
  @ViewChild('tableWrapper') tableWrapper;
  @ViewChild(DatatableComponent) table: DatatableComponent;

  private currentComponentWidth;

  messages = {};

  constructor(private changeDetector: ChangeDetectorRef) {
  }

  @Input()
  parent: BaseListComponent<any> & IPageableList & ISortableList;

  @Input()
  selectionType: SelectionType | undefined = undefined;

  @Input()
  sortedColumns: { prop: string, dir: string }[] = [];

  @Input()
  rowClassFn: Function;

  @Input()
  totalMessage = `$1 total`;

  useExternalPaging() {
    return instanceOfPageableList(this.parent) && this.parent.type != PaginationType.Client;
  }

  useExternalSorting() {
    return instanceOfSortableList(this.parent);
  }

  onFooterChange($event: any) {
    if (this.parent.isLoading) {
      return;
    }
    this.table.onFooterPage($event);
  }
}
