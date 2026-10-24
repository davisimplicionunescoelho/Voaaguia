package com.voaaguia.api;
import com.voaaguia.service.ReportService;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;
import java.util.*;
@RestController @RequestMapping("/api")
public class ReportController {
    private final ReportService reports;
    public ReportController(ReportService reports) { this.reports=reports; }
    @GetMapping("/reports/dashboard") @PreAuthorize("hasRole('LIDER')")
    public Map<String,Object> dashboard(@RequestParam(required=false) String strategyId,@RequestParam(required=false) String projectId) { return reports.dashboard(strategyId,projectId); }
    @GetMapping("/ranking") public List<Map<String,Object>> ranking() { return reports.ranking(); }
}
