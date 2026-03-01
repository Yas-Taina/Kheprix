import { ComponentFixture, TestBed } from '@angular/core/testing';

import { Detalharespecie } from './detalharespecie';

describe('Detalharespecie', () => {
  let component: Detalharespecie;
  let fixture: ComponentFixture<Detalharespecie>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [Detalharespecie],
    }).compileComponents();

    fixture = TestBed.createComponent(Detalharespecie);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
