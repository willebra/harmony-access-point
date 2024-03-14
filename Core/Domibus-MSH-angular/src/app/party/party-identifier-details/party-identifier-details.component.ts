import {Component, Inject, OnInit} from '@angular/core';
import {MAT_DIALOG_DATA, MatDialogRef} from '@angular/material/dialog';
import {EditPopupBaseComponent} from '../../common/edit-popup-base.component';
import {PartyService} from '../support/party.service';
import {PartyIdTypeValidation} from '../support/partyIdTypeValidation';

@Component({
  selector: 'app-party-identifier-details',
  templateUrl: './party-identifier-details.component.html',
  styleUrls: ['./party-identifier-details.component.css'],
  providers: [PartyService]
})
export class PartyIdentifierDetailsComponent extends EditPopupBaseComponent implements OnInit {
  public partyIdTypePattern: string; // = 'urn:oasis:names:tc:ebcore:partyid\\-type:[a-zA-Z0-9_:-]+';
  public partyIdTypeMessage: string; // = 'You should follow the rule: urn:oasis:names:tc:ebcore:partyid-type:[....]';

  partyIdentifier: any;

  constructor(public dialogRef: MatDialogRef<PartyIdentifierDetailsComponent>,
              @Inject(MAT_DIALOG_DATA) public data: any,
              public partyService: PartyService) {
    super(dialogRef, data);

    this.partyIdentifier = data.edit;
  }

  async ngOnInit() {
    const res = await this.getPartyIdTypeValidation();
    console.log('getPartyIdTypeValidation=', res)
  }

  private async getPartyIdTypeValidation(): Promise<PartyIdTypeValidation> {
    const passwordPolicy = await this.partyService.getPartyIdTypeValidation();
    this.partyIdTypePattern = passwordPolicy.pattern;
    this.partyIdTypeMessage = passwordPolicy.validationMessage;
    return passwordPolicy;
  }

  get partyIdTypeName(): string {
    return this.partyIdentifier.partyIdType && this.partyIdentifier.partyIdType.name;
  }

  set partyIdTypeName(val: string) {
    this.partyIdentifier.partyIdType = {...this.partyIdentifier.partyIdType, name: val};
  }


  get partyIdTypeValue(): string {
    return this.partyIdentifier.partyIdType && this.partyIdentifier.partyIdType.value;

  }

  set partyIdTypeValue(val: string) {
    this.partyIdentifier.partyIdType = {...this.partyIdentifier.partyIdType, value: val};
  }

}
