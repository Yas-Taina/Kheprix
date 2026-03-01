import { ComponentFixture, TestBed } from '@angular/core/testing';

import { Listarestudos } from './listarestudos';

describe('Listarestudos', () => {
  let component: Listarestudos;
  let fixture: ComponentFixture<Listarestudos>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [Listarestudos],
    }).compileComponents();

    fixture = TestBed.createComponent(Listarestudos);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
