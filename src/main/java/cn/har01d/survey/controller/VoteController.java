package cn.har01d.survey.controller;

import cn.har01d.survey.dto.ApiResponse;
import cn.har01d.survey.dto.vote.*;
import cn.har01d.survey.service.VoteService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/votes")
public class VoteController {

    private final VoteService voteService;

    public VoteController(VoteService voteService) {
        this.voteService = voteService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<VotePollDto>> createPoll(@Valid @RequestBody VotePollCreateRequest request) {
        VotePollDto poll = voteService.createPoll(request);
        return ResponseEntity.ok(ApiResponse.ok("Vote poll created", poll));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<VotePollDto>> updatePoll(@PathVariable Long id,
                                                               @Valid @RequestBody VotePollCreateRequest request) {
        VotePollDto poll = voteService.updatePoll(id, request);
        return ResponseEntity.ok(ApiResponse.ok("Vote poll updated", poll));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<VotePollDto>> getPoll(@PathVariable Long id) {
        VotePollDto poll = voteService.getPollById(id);
        return ResponseEntity.ok(ApiResponse.ok(poll));
    }

    @GetMapping("/v/{shareId}")
    public ResponseEntity<ApiResponse<VotePollDto>> getPollByShareId(@PathVariable String shareId,
                                                                     HttpServletRequest request) {
        VotePollDto poll = voteService.getPollByShareId(shareId, request);
        return ResponseEntity.ok(ApiResponse.ok(poll));
    }

    @GetMapping("/my")
    public ResponseEntity<ApiResponse<Page<VotePollDto>>> getMyPolls(
            @PageableDefault(sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        Page<VotePollDto> polls = voteService.getMyPolls(pageable);
        return ResponseEntity.ok(ApiResponse.ok(polls));
    }

    @GetMapping("/public")
    public ResponseEntity<ApiResponse<Page<VotePollDto>>> getPublicPolls(
            @PageableDefault(sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        Page<VotePollDto> polls = voteService.getPublicPolls(pageable);
        return ResponseEntity.ok(ApiResponse.ok(polls));
    }

    @PostMapping("/{id}/publish")
    public ResponseEntity<ApiResponse<VotePollDto>> publishPoll(@PathVariable Long id) {
        VotePollDto poll = voteService.publishPoll(id);
        return ResponseEntity.ok(ApiResponse.ok("Vote poll published", poll));
    }

    @PostMapping("/{id}/close")
    public ResponseEntity<ApiResponse<VotePollDto>> closePoll(@PathVariable Long id) {
        VotePollDto poll = voteService.closePoll(id);
        return ResponseEntity.ok(ApiResponse.ok("Vote poll closed", poll));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deletePoll(@PathVariable Long id) {
        voteService.deletePoll(id);
        return ResponseEntity.ok(ApiResponse.ok("Vote poll deleted", null));
    }

    @PostMapping("/v/{shareId}/submit")
    public ResponseEntity<ApiResponse<VotePollDto>> submitVote(
            @PathVariable String shareId,
            @Valid @RequestBody VoteSubmitRequest request,
            HttpServletRequest httpRequest) {
        VotePollDto poll = voteService.submitVote(shareId, request, httpRequest);
        return ResponseEntity.ok(ApiResponse.ok("Vote submitted", poll));
    }
}
