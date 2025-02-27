import {Component, OnInit, ViewChild} from '@angular/core';
import {AbstractControl, NgControl, NgForm} from '@angular/forms';
import {UserValidatorService} from '../../user/support/uservalidator.service';
import {SecurityService} from '../../security/security.service';
import {HttpClient} from '@angular/common/http';
import {AlertService} from '../../common/alert/alert.service';
import {Router} from '@angular/router';
import {Location} from '@angular/common'

@Component({
  templateUrl: './change-password.component.html',
  providers: [UserValidatorService]
})

export class ChangePasswordComponent implements OnInit {

  currentPassword: string;
  password: string;
  passwordConfirmation: string;
  public passwordPattern: string;
  public passwordValidationMessage: string;

  @ViewChild('userForm')
  public userForm: NgForm;

  constructor(private securityService: SecurityService, private http: HttpClient,
              private alertService: AlertService, private router: Router, private _location: Location) {

    this.currentPassword = this.securityService.password;
    this.securityService.password = null;
  }

  async ngOnInit() {
    const role = this.securityService.getCurrentUser().authorities[0];
    const forDomain = role !== SecurityService.ROLE_AP_ADMIN;
    const passwordPolicy = await this.securityService.getPasswordPolicy(forDomain);
    this.passwordPattern = passwordPolicy.pattern;
    this.passwordValidationMessage = passwordPolicy.validationMessage;
  }

  async submitForm() {
    const params = {
      currentPassword: this.currentPassword,
      newPassword: this.password
    };

    try {
      await this.securityService.changePassword(params);
      this.alertService.success('Password successfully changed.');
      this.router.navigate(['/']);
    } catch (error) {
      this.alertService.exception('Password could not be changed.', error);
    }
  }

  public shouldShowErrors(field: NgControl | NgForm | AbstractControl): boolean {
    return (field.touched || field.dirty) && !!field.errors;
  }

  public isFormDisabled() {
    return !this.userForm || this.userForm.invalid || !this.userForm.dirty;
  }

  onCancelClick() {
    this._location.back();
  }

}
