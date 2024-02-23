import {Component, Inject, OnInit} from '@angular/core';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import {YesNoDialogData} from '../DialogData';

@Component({
  selector: 'yes-no-dialog',
  templateUrl: './yes-no-dialog.component.html',
  styleUrls: ['./yes-no-dialog.component.css']
})
export class YesNoDialogComponent {

  constructor(@Inject(MAT_DIALOG_DATA) public data: YesNoDialogData,
              public dialogRef: MatDialogRef<YesNoDialogComponent>) {
  }

}
