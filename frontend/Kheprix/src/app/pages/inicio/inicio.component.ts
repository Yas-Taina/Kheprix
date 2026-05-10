import { Component, OnInit } from "@angular/core";
import { CommonModule } from "@angular/common";
import { Router } from "@angular/router";
import { DashboardService } from "../../core/services/dashboard.service";
import { Dashboard } from "../../models";
import { UtilService } from "../../core/services/util.service";

@Component({
  selector: "app-inicio",
  standalone: true,
  templateUrl: "./inicio.component.html",
  styleUrls: ["./inicio.component.css"],
  imports: [CommonModule],
})
export class InicioComponent implements OnInit {
  dashboard: Dashboard | null = null;
  loading = true;

  constructor(
    public router: Router,
    private dashboardService: DashboardService,
    public util: UtilService,
  ) {}

  ngOnInit() {
    this.dashboardService.getDashboard().subscribe({
      next: (d) => {
        this.dashboard = d;
        this.loading = false;
      },
      error: () => {
        this.loading = false;
      },
    });
  }
}
