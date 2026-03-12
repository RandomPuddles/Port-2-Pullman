Ideas:

* Yakup’s single AI agent doing multiple tasks simultaneously by using MCPs.  
  * If this is done, how is it presented? (Wow factor?)  
    * Web representation of entire process  
  * How do we go about doing this? (Tech stack?)  
    * Python backend, ReactJS frontend.  
* Yakup’s “What files, classes and methods are about to change” idea for Agentic IDEs  
* Auto fill/Auto complete password  
* Better clipboard manager with keyword search (Quick copy/paste)  
  * Has history, multiple copies at once, etc.  
* App that makes my stomach less hurty  
* **“Explain this screen” browser extension**: summarizes a webpage in plain language \+ generates a quick outline

Things we can reimagine:

* Notes app  
  * Automatically link notes together in a graph based on topic/contents  
* Calender  
  * Widget  
  * Add notes during scheduled events  
  * Example notion  
* Zoom  
* Alarms  
  * Maybe not sound?  
  * Maybe not at specific time? Goes off from other criteria?  
    * “When the rain stops, set off my ‘Get the Mail’ alarm”  
  * Alarm speaks to you telling you to do something or name of alarm.  
    * Use the API Voice thing\!\!\!  
* Weather app  
  * Suggests what to wear, maybe it knows your clothes catalog?  
* Less thinking task manager / irl quest tracker, ai powered where you tell the AI what you need to do and some additional info, and the AI will propose a time slot, and user hit accept. Include notifications. Stretch goals, give happy digital currency awards.  
  * Overall idea, is to do your tasks with less thinking as possible. Kinda for people like me with ADHD  
  * User case, user tell AI “What the fuck am I doing, right now?” AI responds: yeah, you supposed to do your homework dummy  
* Copy-Paste  
  * Has history, multiple copies at once, etc.  
* Vibe sensing music speaker that listens to the room and plays music based on the ambient sounds  
  * Ambient music for quiet space  
  * Edm music for lots of commotion and talking  
* Location based notes app  
  * Write down notes and add attachments and images to locations  
  * Good for documenting hangout/hiking/eating spots  
  * Note deals at places on certain days  
  * Browse map of noted locations  
  * Suggest images to attach to noted spots (using their gps metadata)  
  * Can be tailored to app among friends with sharing features  
* Reminder app the ramps up the intensity of the notifications  
  * Simple ping to the app shouting at you what to do  
* Second person shooter (instead of first person shooter)


  


  

Most apps are built on a few basic functions:

* **Input** (how you give it information)  
* **Storage** (how it keeps information)  
* **Organization** (how it structures information)  
* **Retrieval** (how you get it back)  
* **Output** (how it shows/responds)  
* **Triggering** (when it acts)  
* **Interaction model** (tap/list/grid/search/chat/etc.)

To reinvent an app, change one of those **core assumptions**.

## **Judging and Criteria**

*  **Technical Complexity** — Is the project complex? Does it seem like a lot of technical work was done within the work period?  
*  **Polish** — Does the hack actually work? Are things such as UI/UX thought out well?  
*  **"Wow" Factor** — Is the project something exciting and innovative? Does the project exceed the experience level of its team members?  
*  **Usefulness** — Is the project something practical and viable? How would it be received if it was sold as a product today?  
*  **Originality & Creativity** — Is the project original? Can you explain how you implemented the codebase with or without AI? Is it apparent that a lot of thought was put into the project as a whole? How creative is the solution's approach?  
*  **Adherence to Theme** — Does the hack adhere to the theme? Does it implement the theme fully or just partially?

  ### **New Emphasis for Judging**

Judges will be encouraged to look closer at **originality** and weigh it heavier. You will be judged on how well you (and your team) can explain the implementation of your code and how you designed the codebase. Excessive external assistance reduces problem-solving\!

*Source: [https://www.media.mit.edu/projects/your-brain-on-chatgpt/overview/](https://www.media.mit.edu/projects/your-brain-on-chatgpt/overview/)*

## **Hackathon Theme**

# **REINVENTING THE WHEEL**

* Putting a unique twist on a proven concept  
* Trying new things is at the heart of innovation  
* This could mean putting your own spice into a program or concept your team is familiar with  
* Or coming up with a completely new approach to a problem  
    
    
    
    
    
    
    
    
  **Android Tech Stack:**  
  The type of Android tech stack is "native". It is an "Native Android Tech Stack"  
  \- Native:  
  	\- Kotlin  
  	\- Jetpack Compose  
  	\- Coroutines/Flow  
  	\- Android Studio  
  	\- Room  
  	\- Advanced (Possibly optional depending what we want to do):  
  		\- Moshi  
  		\- Ktor Client  
    
    
    
    
    
    
    
    
    
    
    
    
    
* Alarms  
  * Maybe not sound?  
  * Maybe not at specific time? Goes off from other criteria?  
    * “When the rain stops, set off my ‘Get the Mail’ alarm”  
  * Alarm speaks to you telling you to do something or name of alarm.  
    * Use the API Voice thing\!\!\!


## Android Event-Based Reminder app

Android reminder app that can set any condition(s) for when to go off. Option to have the reminder title read out loud by AI voice. Can set alarms and timers. Can add multiple conditions with ADD and OR logic. “Timers” are just reminders with a condition of “after X minutes/seconds”

* Can disable/enable alarms  
* Snooze Button

**Remainder method options:**

- Ping and text message notification (Semi-silent mode)  
- Alarm w/ text message notification (Regular mode)  
- Ping w/ text-to-speech notification (Voice Remainder mode)  
- Alarm w/ text-to-speech notification (Phone call mode)

Example of use:

* “Grab mail from mailbox”  
  * Trigger when: It stops raining  
* “Charge phone”  
  * Trigger when: Battery is below 20%  
* “Take umbrella”  
  * Trigger when: It is raining AND I leave home  
* “Switch laundry”  
  * Trigger when: After 45 minutes  
* “Take medicine”  
  * Trigger when: I arrive home OR it becomes 8:00 PM


  
UI Features:  
Setup Page:

* Title with alarm name at top  
* Section with condition blocks for alarm trigger aligned vertically joined by boolean operators like AND and OR  
* At the bottom of the list of conditions have a add condition box  
  * When the new condition added go to add condition page  
* Boolean operators between conditions can be pressed to open popup window list of operators to select  
* Some conditions can contain numerical  
* Readout option sounds out reminder title until user interaction  
* Ring option sounds alarm until user interaction (If readout also selected, follow by readout sound until user interaction after user interaction)  
* When neither are selected user just gets a ping with text message  
* Trigger Once when selected disables alarm once it’s triggered  
* 


Add Condition Page (or Popup):

* This page opens when the user presses “+ create” in the Manual Setup Window.  
* Conditions are organized by category based on the type of conditions like “weather” or “device attributes” or “time/date” or “x times per x time/days/weeks”  ect.  
* After selecting condition in selected category return to setup page with condition added

Home Page:

* \+ button in top right corner of screen swaps to the Setup Page view  
* AI button in the top right of the screen swaps to the Setup Page view  
* Magnifying Glass button opens a search menu. Can search for key words from the alarm’s titles and conditions.

AI Setup Prompt Popup: Ready for review

* This popup opens when the AI button in the Manual Setup Window is pressed.  
* If the AI button in the Home Window is pressed, it will open the Manual Setup Window, and then the AI Setup Prompt Popup.  
* The AI Setup Prompt Page only contains a text box, prompting “Write prompt”.  
* The purpose of the text box is to prompt an AI assistant to create alarms for the user, per the user’s prompt.

Api options:

* Stock  
* Weather data  
  * Actual weather conditions  
  * Temp  
  * Time of Sunrise / Sunset  
  * Daylight (Same thing as above?)  
* Location  
* Speed  
* Calendar (holidays) (ex: one week before christmas, do shopping)  
*   
* 

MILESTONES:

* How to implement global conditions  
* solution for procrastination  
* conflicting conditions in reminder  
* always true conditions  
* Presets (basic timer)

Should disabled reminders be mixed with enabled or put in a separate page?

Users should be able to add custom conditions that hook up to the Google API, which does a google search to check if the condition is true.

Demo Alarms:

* Timer  
  * In 10 seconds  
* Grab Mail  
  * 1 time a week and Not Raining  
* Alarm  
  * For X time (like one or two minutes from current time so it goes off during demo)  
* Charge Phone  
  * Battery \< 20%  
* Charge phone and grab coat  
  * Battery \< 50% and temp \< 70  
  * Start with battery at 100% and location in a hot place. Show alarm doesn’t go off. Set battery to low percent and location to cooler place. Open debug menu to refresh, alarm goes off\!  
* Sell Stocks  
  * When stock price raises to $50 a share  
* Call for help  
  * Signal strength \>= low  
  * (if stranded in the woods with no reception, you can ensure you don’t miss an opportunity to call)  
* 

  