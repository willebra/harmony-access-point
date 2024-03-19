export class ColumnPickerBase {
  columnSelection: boolean;
  allColumns = [];
  selectedColumns = [];

  changeSelectedColumns(newSelectedColumns: Array<any>) {
    newSelectedColumns.forEach(col => {
      if (!col.width) {
        col.width = 200;
      }
      if (!col.minWidth) {
        col.minWidth = 190;
      }
    });
    this.selectedColumns = newSelectedColumns;
  }

}
