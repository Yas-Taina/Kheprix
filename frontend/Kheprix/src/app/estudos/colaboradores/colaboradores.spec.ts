import { ComponentFixture, TestBed } from '@angular/core/testing';

import { Colaboradores } from './colaboradores';

describe('Colaboradores', () => {
  let component: Colaboradores;
  let fixture: ComponentFixture<Colaboradores>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [Colaboradores],
    }).compileComponents();

    fixture = TestBed.createComponent(Colaboradores);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
