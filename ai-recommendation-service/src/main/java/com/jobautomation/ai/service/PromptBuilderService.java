package com.jobautomation.ai.service;

import com.jobautomation.common.dto.JobApplicationDto;
import com.jobautomation.common.dto.JobDto;
import com.jobautomation.common.dto.ResumeDto;
import com.jobautomation.common.dto.UserDto;
import org.springframework.stereotype.Service;
import java.util.List;

/**
 * Assembles context-rich prompts for the Gemini LLM.
 * Extracted from the monolith's AiAgentServiceImpl.askAgent() method.
 */
@Service
public class PromptBuilderService {

    /**
     * Builds a comprehensive context prompt for the AI agent.
     *
     * @param userPrompt      the raw user question
     * @param jobs            current jobs in the job catalog
     * @param applications    current job application records
     * @param user            the requesting user's profile (nullable)
     * @param resume          the user's resume content (nullable)
     * @return the full context-enriched prompt string
     */
    public String buildAgentPrompt(String userPrompt, List<JobDto> jobs,
                                    List<JobApplicationDto> applications,
                                    UserDto user, ResumeDto resume) {
        StringBuilder context = new StringBuilder();
        context.append("You are a specialized Job Automation Assistant.\n");
        context.append("Your role is to help users with job searches, resume analysis, and career advice based on the data provided.\n");
        context.append("Strictly refuse to answer questions unrelated to jobs, resumes, or career development.\n");
        context.append("Here is the current job data in the database:\n");
        context.append("--------------------------------------------------\n");

        if (jobs != null && !jobs.isEmpty()) {
            jobs.stream().limit(20).forEach(j ->
                    context.append(String.format("ID: %s, Title: %s, Company: %s, Location: %s, Applied: %s\n",
                            j.getId(), j.getTitle(), j.getCompany(), j.getLocation(), j.isApplied())));
        } else {
            context.append("No jobs found in the database.\n");
        }

        context.append("--------------------------------------------------\n");
        context.append("Current Job Applications Status:\n");
        if (applications != null && !applications.isEmpty()) {
            applications.stream().limit(20).forEach(app ->
                    context.append(String.format("App ID: %s, Job ID: %s, Status: %s, Applied: %s\n",
                            app.getId(), app.getJobId(), app.getStatus(), app.isJobApplied())));
        } else {
            context.append("No active applications found.\n");
        }

        if (user != null) {
            context.append("--------------------------------------------------\n");
            context.append("User Context:\n");
            context.append(String.format("Name: %s, Email: %s, Experience: %d years, Roles: %s\n",
                    user.getName(), user.getEmail(), user.getExperienceYears(), user.getPreferredRoles()));
            if (resume != null && resume.getContent() != null) {
                context.append("Latest Resume Content: ").append(resume.getContent()).append("\n");
            }
        }

        context.append("--------------------------------------------------\n");
        context.append("Answer the following question based ONLY on the data provided and your recruitment expertise.\n");
        context.append("User Question: ").append(userPrompt);
        return context.toString();
    }

    /**
     * Builds a resume analysis prompt.
     */
    public String buildResumeAnalysisPrompt(String resumeContent, List<JobDto> jobs) {
        String jobSummary = jobs.stream()
                .limit(5)
                .map(j -> j.getTitle() + ": " + j.getDescription())
                .collect(java.util.stream.Collectors.joining("\n---\n"));
        return String.format(
                "Analyze this resume and suggest improvements based on these jobs:\nResume: %s\n\nJobs:\n%s",
                resumeContent != null ? resumeContent : "No resume found.",
                jobSummary.isEmpty() ? "No jobs available." : jobSummary);
    }
}
