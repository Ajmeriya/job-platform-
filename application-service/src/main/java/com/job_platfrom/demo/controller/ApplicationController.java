package com.job_platfrom.demo.controller;

import com.job_platfrom.demo.dto.ApplicationResponse;
import com.job_platfrom.demo.dto.ApplicationRoundUpdateRequest;
import com.job_platfrom.demo.dto.ApplyRequest;
import com.job_platfrom.demo.dto.ResumeReviewUpdateRequest;
import com.job_platfrom.demo.dto.ResumeUploadResponse;
import com.job_platfrom.demo.service.ApplicationService;
import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

@RestController
@RequestMapping("/applications")
@RequiredArgsConstructor
public class ApplicationController {

    private final ApplicationService applicationService;
    private static final Set<String> ALLOWED_FILE_EXTENSIONS = Set.of("pdf", "doc", "docx");

    @Value("${app.resume-upload-dir:uploads/resumes}")
    private String resumeUploadDir;

    @PostMapping
    public ResponseEntity<?> apply(@RequestBody ApplyRequest request, Authentication authentication) {
        try {
            String email = extractEmail(authentication);
            ApplicationResponse response = applicationService.apply(email, request);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(ex.getMessage());
        }
    }

    @GetMapping("/me")
    public ResponseEntity<?> getMyApplications(Authentication authentication) {
        try {
            String email = extractEmail(authentication);
            List<ApplicationResponse> responses = applicationService.getMyApplications(email);
            return ResponseEntity.ok(responses);
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(ex.getMessage());
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getById(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(applicationService.getById(id));
        } catch (IllegalArgumentException ex) {
            if (ex.getMessage() != null && ex.getMessage().startsWith("Application not found")) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ex.getMessage());
            }
            return ResponseEntity.badRequest().body(ex.getMessage());
        }
    }

    @GetMapping
    public ResponseEntity<List<ApplicationResponse>> getAllApplications() {
        return ResponseEntity.ok(applicationService.getAllApplications());
    }

    @GetMapping("/jobs/{jobId}")
    public ResponseEntity<?> getApplicationsByJob(@PathVariable Long jobId) {
        try {
            return ResponseEntity.ok(applicationService.getApplicationsByJobId(jobId));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(ex.getMessage());
        }
    }

    @PostMapping("/{id}/resume-review")
    public ResponseEntity<?> updateResumeReview(@PathVariable Long id, @RequestBody ResumeReviewUpdateRequest request) {
        try {
            return ResponseEntity.ok(applicationService.updateResumeReview(id, request));
        } catch (IllegalArgumentException ex) {
            if (ex.getMessage() != null && ex.getMessage().startsWith("Application not found")) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ex.getMessage());
            }
            return ResponseEntity.badRequest().body(ex.getMessage());
        }
    }

    @PostMapping("/{id}/rounds")
    public ResponseEntity<?> updateRound(@PathVariable Long id, @RequestBody ApplicationRoundUpdateRequest request) {
        try {
            return ResponseEntity.ok(applicationService.updateRound(id, request));
        } catch (IllegalArgumentException ex) {
            if (ex.getMessage() != null && ex.getMessage().startsWith("Application not found")) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ex.getMessage());
            }
            return ResponseEntity.badRequest().body(ex.getMessage());
        }
    }

    @PostMapping(value = "/resumes", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> uploadResume(@RequestParam("file") MultipartFile file) {
        try {
            if (file == null || file.isEmpty()) {
                return ResponseEntity.badRequest().body("Resume file is required");
            }

            String originalName = StringUtils.cleanPath(file.getOriginalFilename() == null ? "resume" : file.getOriginalFilename());
            String extension = getFileExtension(originalName);
            if (!ALLOWED_FILE_EXTENSIONS.contains(extension.toLowerCase())) {
                return ResponseEntity.badRequest().body("Only pdf, doc, docx files are allowed");
            }

            Path uploadPath = Paths.get(resumeUploadDir).toAbsolutePath().normalize();
            Files.createDirectories(uploadPath);

            String storedFileName = UUID.randomUUID() + "." + extension;
            Path target = uploadPath.resolve(storedFileName);
            Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);

            String fileUrl = ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/applications/resumes/")
                .path(storedFileName)
                .toUriString();

            ResumeUploadResponse response = ResumeUploadResponse.builder()
                .fileName(originalName)
                .storedFileName(storedFileName)
                .fileUrl(fileUrl)
                .build();

            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (IOException ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to store resume file");
        }
    }

    @GetMapping("/resumes/{fileName:.+}")
    public ResponseEntity<?> downloadResume(@PathVariable String fileName, HttpServletRequest request) {
        try {
            if (fileName.contains("..") || fileName.contains("/") || fileName.contains("\\")) {
                return ResponseEntity.badRequest().body("Invalid file name");
            }

            Path uploadPath = Paths.get(resumeUploadDir).toAbsolutePath().normalize();
            Path filePath = uploadPath.resolve(fileName).normalize();

            Resource resource = new UrlResource(filePath.toUri());
            if (!resource.exists() || !resource.isReadable()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("File not found");
            }

            String contentType = request.getServletContext().getMimeType(resource.getFile().getAbsolutePath());
            if (contentType == null) {
                contentType = MediaType.APPLICATION_OCTET_STREAM_VALUE;
            }

            return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + resource.getFilename() + "\"")
                .body(resource);
        } catch (IOException ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to read resume file");
        }
    }

    private String getFileExtension(String filename) {
        int idx = filename.lastIndexOf('.');
        if (idx < 0 || idx == filename.length() - 1) {
            return "";
        }
        return filename.substring(idx + 1);
    }

    private String extractEmail(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new IllegalArgumentException("Unauthorized");
        }

        String email = authentication.getName();
        if (email == null || email.isBlank() || "anonymousUser".equalsIgnoreCase(email)) {
            throw new IllegalArgumentException("Unauthorized");
        }

        return email;
    }
}
