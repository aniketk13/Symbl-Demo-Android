# Symbl-Demo-Android
This is a Symbl.ai Demo Application to help the beginners get started.
Specifically, this helps in generating Video Transcriptions in just one click.

To use this application
- Fork the repo and clone it in your system
- Open the local repo in Android Studio
- Go to symbl.ai and create an account for free
- Get your App Id and Secret Key and paste it in local.properties file as:


`Symbl_App_ID= {YOUR_APP_ID}`
<br/>
`Symbl_App_Secret= {YOUR_SECRET_KEY}`

**Advisable not to expose your API Keys publicly, putting them in local.properies ignores it to get committed in GitHub**
- Then click on Run 🚀

Open `MainActivity.kt` to get an overview of the complete code with explainations about all the functionalities

Order in which the application executes 📲:
- Access Token generation (https://docs.symbl.ai/docs/developer-tools/authentication)
- Processing the video url and generating its Conversation as well as Job-Id (https://docs.symbl.ai/docs/async-api/code-snippets/receive-speech-to-text-and-ai-insights/)
- Extracting all the dialogues and append them to make the transcription 
- Showing the Transcription on screen with a Copy to Clipboard feature

If you need any help, join [Symbl.ai Slack](https://symbldotai.slack.com/join/shared_invite/zt-4sic2s11-D3x496pll8UHSJ89cm78CA#/), and drop your query in the support channel

Connect with me [@AniketKhajanchi](https://twitter.com/AniketKhajanchi)
