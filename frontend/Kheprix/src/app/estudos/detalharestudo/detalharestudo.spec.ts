import { ComponentFixture, TestBed } from '@angular/core/testing';

import { Detalharestudo } from './detalharestudo';

describe('Detalharestudo', () => {
  let component: Detalharestudo;
  let fixture: ComponentFixture<Detalharestudo>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [Detalharestudo],
    }).compileComponents();

    fixture = TestBed.createComponent(Detalharestudo);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
