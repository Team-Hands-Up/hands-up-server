package com.back.handsUp.controller;

import com.back.handsUp.baseResponse.BaseException;
import com.back.handsUp.baseResponse.BaseResponse;
import com.back.handsUp.domain.inquiry.Inquiry;
import com.back.handsUp.domain.report.Report;
import com.back.handsUp.dto.inquiryAndReport.InquiryDto;
import com.back.handsUp.dto.inquiryAndReport.ReportDto;
import com.back.handsUp.service.InquiryReportService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

@RequiredArgsConstructor
@RestController
@Slf4j
@RequestMapping("/help")
public class InquiryReportController {

    private final InquiryReportService inquiryReportService;

    @PostMapping("/inquiry")
    public BaseResponse<String> inquiry(Principal principal, @RequestBody InquiryDto.PostInquiryInfo postInquiryInfo) {
        try {
            this.inquiryReportService.inquiry(principal, postInquiryInfo);
            return new BaseResponse<>("문의가 접수되었습니다.");
        } catch (BaseException e) {
            return new BaseResponse<>(e.getStatus());
        }
    }

    @PostMapping("/report")
    public BaseResponse<String> report(Principal principal, @RequestBody ReportDto.PostReportContent postReportContent) {
        try {
            this.inquiryReportService.report(principal, postReportContent);
            return new BaseResponse<>("신고가 접수되었습니다.");
        } catch (BaseException e) {
            return new BaseResponse<>(e.getStatus());
        }
    }

    //문의 조회(관리자용)
    @GetMapping("/inquiry")
    public BaseResponse<List<InquiryDto.PostInquiryInfo>> getInquiry(Principal principal){
        try{
            List<InquiryDto.PostInquiryInfo> getInquiry = this.inquiryReportService.getInquiry(principal);
            return new BaseResponse<>(getInquiry);
        } catch (BaseException e) {
            return new BaseResponse<>(e.getStatus());
        }
    }

    //신고 조회(관리자용)
    @GetMapping("/report")
    public BaseResponse<List<ReportDto.GetReport>> getReport(Principal principal){
        try{
            List<ReportDto.GetReport> getReport = this.inquiryReportService.getReport(principal);
            return new BaseResponse<>(getReport);
        } catch (BaseException e) {
            return new BaseResponse<>(e.getStatus());
        }
    }
}
