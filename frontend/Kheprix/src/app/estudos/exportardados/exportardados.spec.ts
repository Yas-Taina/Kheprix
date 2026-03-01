import { ComponentFixture, TestBed } from '@angular/core/testing';

import { Exportardados } from './exportardados';

describe('Exportardados', () => {
  let component: Exportardados;
  let fixture: ComponentFixture<Exportardados>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [Exportardados],
    }).compileComponents();

    fixture = TestBed.createComponent(Exportardados);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
