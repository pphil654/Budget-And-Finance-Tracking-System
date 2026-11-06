package com.bfts.controller;

import com.bfts.entity.user;
import com.bfts.repository.UserRepository;

import jakarta.servlet.http.HttpSession;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class LoginController {

    @Autowired
    private UserRepository userRepository;

    // Open login page
    @GetMapping("/")
    public String showHomePage() {
        return "login";
    }

    // Open login page
    @GetMapping("/login")
    public String showLoginPage() {
        return "login";
    }

    // Process login
    @PostMapping("/login")
    public String login(
            @RequestParam("username") String username,
            @RequestParam("password") String password,
            Model model,
            HttpSession session) {

        user existingUser = userRepository.findByUsername(username)
                .orElse(null);

        // Username does not exist
        if (existingUser == null) {
            model.addAttribute("error", "Invalid username or password.");
            return "login";
        }

        // Account is inactive
        if (!existingUser.isActive()) {
            model.addAttribute("error", "Your account is inactive.");
            return "login";
        }

        // Wrong password
        if (!existingUser.getPassword().equals(password)) {
            model.addAttribute("error", "Invalid username or password.");
            return "login";
        }

        // Store logged-in user information in session
        session.setAttribute("loggedInUserId", existingUser.getId());
        session.setAttribute("loggedInUsername", existingUser.getUsername());
        session.setAttribute("loggedInFullName", existingUser.getFullName());

        if (existingUser.getRole() != null) {
            session.setAttribute(
                    "loggedInRole",
                    existingUser.getRole().getName()
            );
        }

        // Login successful
        return "redirect:/dashboard";
    }
    // Logout
    @GetMapping("/logout")
    public String logout(HttpSession session) {

        session.invalidate();

        return "redirect:/login";
    }
}