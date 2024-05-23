import {Component, Inject, ViewChild} from '@angular/core';
import { MatDialogRef } from '@angular/material/dialog';
import {TrustStoreService} from '../support/trustore.service';
import {AbstractControl, UntypedFormBuilder, UntypedFormControl, UntypedFormGroup, NgControl, NgForm, Validators} from '@angular/forms';
import {MAT_DIALOG_DATA} from '@angular/material/dialog';

@Component({
  selector: 'app-certificate-upload',
  templateUrl: './certificate-upload.component.html',
  styleUrls: ['./certificate-upload.component.css'],
  providers: [TrustStoreService]
})
export class CertificateUploadComponent {

  truststoreForm: UntypedFormGroup;
  selectedFileName: string;
  fileSelected = false;

  @ViewChild('fileInput') fileInput;

  constructor(public dialogRef: MatDialogRef<CertificateUploadComponent>, private fb: UntypedFormBuilder, @Inject(MAT_DIALOG_DATA) public data: any) {
    this.truststoreForm = fb.group({
      'alias': new UntypedFormControl('', Validators.required),
    });
  }

  public isFormValid(): boolean {
    return this.truststoreForm.valid && this.fileSelected;
  }

  public async submit() {
    if (!this.isFormValid()) {
      return;
    }
    const fileToUpload = this.fileInput.nativeElement.files[0];
    const alias = this.truststoreForm.get('alias').value;
    const result = {
      file: fileToUpload,
      alias: alias
    };
    this.dialogRef.close(result);
  }

  selectFile() {
    const fi = this.fileInput.nativeElement;
    const file = fi.files[0];
    this.selectedFileName = file.name;

    this.fileSelected = fi.files.length != 0;
  }

  public shouldShowErrors(field: NgControl | NgForm | AbstractControl): boolean {
    return (field.touched || field.dirty) && !!field.errors;
  }
}
