import { ComponentFixture, TestBed } from '@angular/core/testing';

import { Convites } from './convites';

describe('Convites', () => {
  let component: Convites;
  let fixture: ComponentFixture<Convites>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [Convites],
    }).compileComponents();

    fixture = TestBed.createComponent(Convites);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
