# Peer-to-Peer Chat Application
This is a console-based, LAN-supported Peer-to-Peer Chat Application developed in Core Java. It uses TCP/IP socket programming, multithreading, concurrency control, and exception handling to enable real-time communication between users on the same local network. Jansi libraries have been incorporated to enhance the console appearance.

## Features

- **Peer-to-Peer Communication**: Allows direct communication between users over a LAN.
- **Console-Based Interface**: Lightweight and straightforward console interface with enhanced visuals using Jansi.
- **Broadcast and Private Messaging**: Both broadcast and private messaging options are available.
- **Multithreading**: Ensures smooth operation with multiple clients by handling each user in a separate thread.
- **Concurrency Control**: Manages simultaneous messaging efficiently without conflicts.
- **Exception Handling**: Provides stability with robust error-handling for network and input/output exceptions.

## Technologies Used

- **Core Java**: Developed using core Java functionalities.
- **TCP/IP Socket Programming**: Manages network communication over TCP.
- **Multithreading**: Enables concurrent handling of multiple users.
- **Jansi Library**: Adds colors and styling to the console output.

## Setup and Installation

1. **Clone the Repository**  
   ```bash
   git clone https://github.com/anushka027/PeerToPeerApp.git
   cd PeerToPeerApp
   ```

2. **Add Jansi Library**  
   Ensure you have the Jansi library added to your project. You can download it from [Jansi GitHub](https://github.com/fusesource/jansi) or use it through Maven.

3. **Configure Network Settings**  
   Adjust the subnet mask in the code if required to match your LAN’s network configuration. 

4. **Compile and Run the Server**  
   Start the server on your machine:
   ```bash
   javac Peer.java
   java Peer
   ```

5. **Start the Client**  
   The Client will start automatically as soon as the app runs. Repeat on different machines connected to the same LAN, do not open multiple instances on the same machine for testing as it will connect one IP Address only once.

## How to Use

1. **Start the Server**: Run `Peer.java` to start the chat server.
2. **Connect with other Clients**: Select from the list of available clients to start communication or choose broadcast messaging.
3. **Chat**: Type messages in the console to communicate. The Jansi library enhances the appearance of console messages for improved readability.

## Future Improvements

- Add a GUI for a more user-friendly experience.
- Enhance security with message encryption.
- Implement file-sharing capabilities.

## Contributing

Contributions are welcome! Please fork the repository and create a pull request with your changes.
