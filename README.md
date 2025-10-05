# FinTrack: Your Personal Finance Companion

*Empower your financial journey with intuitive tracking and smart insights.*

[![GitHub Repo](https://img.shields.io/badge/GitHub-Repository-blue?logo=github)](https://github.com/Viranya2006/FinTrack)  
[![Language](https://img.shields.io/badge/Language-Java-orange?logo=java)](https://github.com/Viranya2006/FinTrack)  
[![Platform](https://img.shields.io/badge/Platform-Android-green?logo=android)](https://github.com/Viranya2006/FinTrack)  
[![Firebase](https://img.shields.io/badge/Backend-Firebase-yellow?logo=firebase)](https://firebase.google.com/)  
[![License](https://img.shields.io/badge/License-MIT-lightgrey)](LICENSE) <!-- Suggest adding MIT license -->

## 🌟 Overview
FinTrack is a sleek, modern Android application designed to simplify personal finance management. Whether you're tracking daily expenses, setting budgets, or working towards savings goals, FinTrack provides a seamless experience with real-time syncing, secure authentication, and AI-powered suggestions. Built for users who want clarity and control over their finances without the complexity.

This open-source project is perfect for developers looking to contribute to mobile finance tools or learn Android development with Firebase integration.

## 🚀 Key Features
- **Secure Authentication**: Easy signup, login, password reset, and app lock for privacy.
- **Account Management**: Add, edit, and monitor multiple accounts (e.g., bank, cash, credit).
- **Transaction Tracking**: Log income and expenses with categories, dates, and notes.
- **Budgeting Tools**: Set custom budgets per category and get alerts on overspending.
- **Savings Goals**: Create personalized goals with progress trackers and milestones.
- **Smart Suggestions**: AI-driven tips to optimize spending and save more (powered by simple algorithms).
- **Intuitive Dashboard**: Visualize your finances with charts, summaries, and recent activity.
- **Offline Support**: Basic functionality works offline, with sync on reconnect (via Firebase).

## 🛠 Tech Stack
FinTrack leverages modern Android tools for a robust and performant app:

| Category          | Technologies/Tools                  |
|-------------------|-------------------------------------|
| **Language**     | Java                               |
| **Build System** | Gradle (Kotlin DSL)                |
| **Backend**      | Firebase Authentication & Realtime Database |
| **UI Framework** | AndroidX, Material Design, RecyclerView, Fragments |
| **Security**     | ProGuard for code obfuscation      |
| **Testing**      | JUnit (unit & instrumented tests)  |

## 📱 Screenshots
Explore FinTrack's clean interface:

| Home Dashboard                  | Transaction List                | Budget Overview                 |
|---------------------------------|---------------------------------|---------------------------------|
| <img width="264" height="758" alt="Home" src="https://github.com/user-attachments/assets/3eb59463-8f1d-4c31-a41e-1a76fdaf7d22" /> | <img width="264" height="558" alt="Incomes" src="https://github.com/user-attachments/assets/e060d771-0fd1-4198-b43a-e2a4350e20a7" /> | <img width="264" height="558" alt="Monthly Budget" src="https://github.com/user-attachments/assets/2770f191-9c41-4e54-9430-f49b0cb677ab" /> |



## 🔧 Installation
Get FinTrack up and running in minutes:

1. **Clone the Repository**:
   ```
   git clone https://github.com/Viranya2006/FinTrack.git
   ```
   
2. **Open in Android Studio**:
   - Launch Android Studio and open the project folder.

3. **Configure Firebase**:
   - Create a Firebase project at [console.firebase.google.com](https://console.firebase.google.com).
   - Download `google-services.json` and place it in the `app/` directory.

4. **Build and Run**:
   - Sync Gradle dependencies.
   - Build the project (Ctrl + F9 on Windows/Linux, Cmd + F9 on macOS).
   - Run on an emulator or physical device (Android API 21+ recommended).

*Note: Ensure you have the Android SDK and necessary emulators installed.*

## 📖 Usage Guide
1. **Launch the App**: Start with the splash screen leading to login/signup.
2. **Set Up Profile**: Create an account and enable app lock for security.
3. **Add Data**:
   - Use bottom sheets to quickly add transactions, budgets, or goals.
   - Categorize expenses (e.g., food, travel) for better insights.
4. **Monitor Progress**: View summaries on the home fragment; drill down into transactions or profiles.
5. **Get Suggestions**: Check the suggestions adapter for personalized finance tips.

For advanced users: Customize categories in `CategoriesActivity.java` or extend models in the `model/` package.

## 🤝 Contributing
We welcome contributions to make FinTrack even better! Here's how to get involved:

1. Fork the repository.
2. Create a feature branch: `git checkout -b feature/AmazingFeature`.
3. Commit your changes: `git commit -m 'Add some AmazingFeature'`.
4. Push to the branch: `git push origin feature/AmazingFeature`.
5. Open a Pull Request.

Please follow our [Code of Conduct](CODE_OF_CONDUCT.md) and check open issues for ideas. Focus on areas like:
- Adding charts (e.g., via MPAndroidChart).
- Implementing export to CSV/PDF.
- Enhancing accessibility.

## 🛤️ Roadmap
Future enhancements planned:
- Integration with bank APIs for auto-transactions.
- Dark mode toggle.
- Multi-currency support.
- Advanced analytics with machine learning.
- iOS version (cross-platform migration).

Stay tuned—star the repo for updates!

## ❓ FAQ
- **Is FinTrack free?** Yes, completely open-source and free to use.
- **How secure is my data?** Data is stored securely in Firebase; enable app lock for extra protection.
- **Can I use it offline?** Partial support; full sync requires internet.
- **Issues?** Report bugs via GitHub Issues.

## 📝 License
This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details. (If not present, consider adding one to encourage contributions.)

## 🙏 Acknowledgments
- Thanks to the Android community and Firebase team for excellent tools.
- Inspired by popular finance apps like Mint and YNAB.

*FinTrack: Because every penny counts. Track smart, live better! 💰*

---

<p align="center">
  Made with ❤️ in Sri Lanka
</p>
