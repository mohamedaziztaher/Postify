# Postify - Social Media Android App

## Overview
Postify is a modern social media Android application that allows users to share moments, connect with others, and engage with content. Built with Material Design 3 principles and Firebase backend, it provides a seamless and intuitive user experience.

## Features

### Authentication
- Email/Password registration and login
- Profile completion flow
- Secure authentication using Firebase Auth
- Logout confirmation dialog

### Posts
- Create posts with images and captions
- Like and comment on posts
- Real-time post updates
- Delete posts with confirmation
- Image upload to Cloudinary
- Dynamic like button states
- Click on posts to view author profiles

### Profile Management
- Customizable user profiles
- Profile picture upload
- Bio and username editing
- View personal posts grid
- View other users' profiles
- Camera and gallery image selection

### Social Features
- Interactive post feed
- Real-time comment system
- Like/Unlike posts with visual feedback
- User-to-user profile navigation
- Empty state handling for no posts

### UI/UX
- Material Design 3 implementation
- Responsive layouts
- Dynamic color theming
- Like animation and color changes
- Improved comment section UI
- Modern bottom sheets for image picking
- Confirmation dialogs for important actions

## Technical Stack

### Architecture
- MVVM Architecture Pattern
- Fragment-based navigation
- ViewBinding for view interactions
- Clean separation of concerns

### Backend Services
- Firebase Authentication
- Firebase Realtime Database
- Cloudinary for image storage

### Libraries
- Android Navigation Component
- Glide for image loading
- Material Design Components
- CloudinaryAndroid
- AndroidX libraries

## Setup Instructions
1. Clone the repository
2. Open project in Android Studio
3. Configure Firebase:
   - Create a new Firebase project
   - Add your `google-services.json`
   - Enable Authentication, Realtime Database
4. Configure Cloudinary:
   - Add Cloudinary credentials
   - Configure upload preset
5. Build and run the project



