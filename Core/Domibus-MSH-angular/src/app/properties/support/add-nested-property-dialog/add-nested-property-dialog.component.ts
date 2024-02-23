import {Component, Inject, ViewChild} from '@angular/core';
import {MAT_DIALOG_DATA, MatDialogRef} from '@angular/material/dialog';
import {PropertyModel} from '../properties.service';
import {NgForm, UntypedFormGroup} from '@angular/forms';
import {EditPopupBaseComponent} from '../../../common/edit-popup-base.component';

/**
 * @author Ion Perpegel
 * @since 5.0
 */
@Component({
  selector: 'app-session-expired-dialog',
  templateUrl: './add-nested-property-dialog.component.html'
})
export class AddNestedPropertyDialogComponent extends EditPopupBaseComponent {
  @ViewChild('editForm')
  public editForm: NgForm | UntypedFormGroup;

  property: PropertyModel;
  propertySuffix = '';
  propertyValue: string;

  constructor(public dialogRef: MatDialogRef<AddNestedPropertyDialogComponent>, @Inject(MAT_DIALOG_DATA) public data: any) {
    super(dialogRef, data);

    this.property = data;
  }

  protected getDialogResult(): any {
    return {propertySuffix: this.propertySuffix, propertyValue: this.propertyValue};
  }
}
