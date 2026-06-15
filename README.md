# Sneh - Holistic Wellness & Fertility Tracker

Sneh is a comprehensive Android application designed to provide holistic support for wellness, fertility tracking, and family planning. Built with a modern architecture in Kotlin, Sneh offers a suite of tools ranging from AI-powered guidance to expert community interactions, helping users make informed health decisions.

## Features

*   **Dashboard:** A central hub providing an overview of your daily wellness, upcoming appointments, and key health metrics.
*   **AI Chat Assistant:** A smart, AI-driven chat feature that provides personalized guidance, answers queries related to health, and offers support.
*   **Calendar & Tracking:** Easily log your daily symptoms, track cycles, and manage appointments.
*   **Fertility Plan:** Tailored plans and tracking mechanisms to support your family planning journey.
*   **What If Simulator:** A unique predictive tool that allows users to simulate various health scenarios and see potential outcomes.
*   **Expert Community:** Connect with a supportive community and health experts to share experiences and seek advice.
*   **Mind Wellness:** Resources and tracking for mental health, meditation, and stress management.
*   **Physical Fitness:** Workout tracking and fitness routines customized to your health goals.
*   **Nutrition:** Diet planning, meal logging, and nutritional guidance.
*   **Physician Report:** Manage, organize, and view your medical reports in one place.
*   **Authentication:** Secure Login and Signup, including Google Sign-In support.

## Tech Stack

*   **Language:** Kotlin
*   **UI:** XML Layouts / Views (with modern Material Design principles)
*   **Architecture:** Standard Android Architecture
*   **Build System:** Gradle (Kotlin DSL)

## Project Structure

The project follows a standard modular approach within the `app` module:

*   `ui.main`: Contains the core feature fragments (Dashboard, Calendar, AI Chat, etc.).
*   `auth`: Handles user authentication, login, and registration.
*   `onboarding`: Initial app setup and user onboarding flows.
*   `ai`: Logic for AI Chat features.
*   `data`: Data layer for models and repositories.
*   `core`: Base classes and core application logic.
*   `utils`: Helper classes and extensions.

## Getting Started

To run this project locally:

1.  **Clone the repository.**
2.  **Open the project** in Android Studio.
3.  **Sync Project with Gradle Files** to download all necessary dependencies.
4.  **Run the app** on an emulator or a physical Android device.

## Prerequisites

*   Android Studio (Latest Version Recommended)
*   JDK 17+ (as per modern Gradle requirements)
*   An active internet connection (for syncing Gradle and utilizing AI/Community features).

