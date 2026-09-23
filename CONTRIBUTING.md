# Contributing

Thank you for your interest in contributing to Reminder! This document provides guidelines for contributing to the project.

## Getting Started

### Prerequisites
- JDK 21 (Temurin or OpenJDK recommended)
- Android SDK with Platform 36 (Android 16) and Build-Tools 35.0.0+
- Git

### Setup
1. Fork the repository
2. Clone your fork: `git clone https://github.com/YOUR_USERNAME/android-reminder.git`
3. Navigate to the project: `cd android-reminder`
4. Open in Android Studio or use command line tools

## Development Workflow

### Branching
- Create a feature branch: `git checkout -b feature/your-feature-name`
- Keep branches focused on a single feature or fix

### Code Style
- Follow Kotlin coding conventions
- Use meaningful variable and function names
- Add comments for complex logic
- Keep functions small and focused

### Testing
- Run unit tests: `./gradlew testDebugUnitTest`
- Write tests for new use cases and business logic
- Ensure all tests pass before committing

### Commit Messages
Follow conventional commit format:
- `feat: add new feature`
- `fix: resolve bug description`
- `refactor: improve code structure`
- `docs: update documentation`

Example:
```
feat: add category management in settings

- Add CategoriesScreen with CRUD operations
- Implement category creation with color picker
- Add category deletion with confirmation dialog
```

## Project Guidelines

### Architecture
- Follow the established MVI pattern
- Keep business logic in use cases
- Use repositories for data access
- Maintain separation of concerns

### UI/UX
- Follow Material 3 design guidelines
- Ensure accessibility considerations
- Test on different screen sizes
- Consider dark mode and AMOLED theme

### Code Quality
- Write clean, readable code
- Avoid code duplication
- Use existing utilities and helpers
- Follow the project's coding patterns

### Privacy & FOSS
- Maintain 100% FOSS commitment
- No proprietary dependencies
- No tracking or analytics
- Data stays on device

## Submitting Changes

1. Update your branch: `git pull origin main`
2. Run tests: `./gradlew testDebugUnitTest`
3. Commit your changes with clear messages
4. Push to your fork: `git push origin feature/your-feature-name`
5. Create a Pull Request on GitHub

### Pull Request Checklist
- [ ] Code follows project style guidelines
- [ ] Tests pass locally
- [ ] Documentation is updated if needed
- [ ] No breaking changes without discussion
- [ ] Commit messages are clear

## Reporting Issues

When reporting bugs:
1. Use GitHub Issues
2. Provide clear description
3. Include steps to reproduce
4. Specify Android version and device
5. Add screenshots if applicable

## Feature Requests

For feature requests:
1. Check existing issues first
2. Provide clear use case
3. Explain why it's valuable
4. Consider implementation complexity

## Code Review

All contributions go through code review:
- Be open to feedback
- Respond to review comments
- Make requested changes
- Keep discussions constructive

## Questions

Feel free to ask questions in:
- GitHub Issues (for bugs/features)
- Pull Request discussions (for code-specific questions)
- GitHub Discussions (for general questions)

## License

By contributing, you agree that your contributions will be licensed under the GNU GPLv3.
