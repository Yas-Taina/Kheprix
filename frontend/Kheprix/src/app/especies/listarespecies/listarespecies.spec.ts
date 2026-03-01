import { ComponentFixture, TestBed } from '@angular/core/testing';

import { Listarespecies } from './listarespecies';

describe('Listarespecies', () => {
  let component: Listarespecies;
  let fixture: ComponentFixture<Listarespecies>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [Listarespecies],
    }).compileComponents();

    fixture = TestBed.createComponent(Listarespecies);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
