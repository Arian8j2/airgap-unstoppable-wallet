> [!WARNING]
> This fork is a work in progress. You may encounter bugs and unexpected behavior during use.  
> **Please use it at your own risk.**  
> FYI I'm not a Kotlin or Android developer, so the code might not be in a good quality

# Air Gap Fork
This fork adds air gap feature like [Airgap Wallet](https://github.com/airgap-it/airgap-wallet) to some coins in [Unstoppable Wallet](https://github.com/horizontalsystems/unstoppable-wallet-android)  
### Current supported actions:
* Sending any token on EVM chains (Ethereum, Bsc, Polygon, Avalanche, Optimism, Base, ArbitrumOne, Gnosis, Fantom)
* Speed up or cancel transactions on EVM chains
* Sending Bitcoin
* Sending Solana

# Setup
To get started, you will need two devices:

1. **Online Wallet:** This device is regularly used and must be connected to the internet when you want to send transactions.
2. **Offline Wallet:** This device must never connect to the internet. Ensure it has no SIM card installed and that Bluetooth is turned off.

By adhering to these guidelines, you can ensure the security of your offline wallet while utilizing the online wallet for transactions.

### Steps:
1. **Create or Import a Wallet:**  
   On the offline device, create or import a standard wallet.

2. **Create a Watch Account:**  
   On the online device, create a watch account using the receive address from your offline wallet.

3. **Transfer Tokens or Coins:**  
   - On your online wallet, press the `AirGap Send` button.
   - Fill in the destination address and any other required fields.
   - A QR code will be generated containing the data needed for the offline device to create a valid transaction.

4. **Scan the QR Code:**  
   - On your offline wallet, click the scan button in the top right corner of the balance page.
   - Scan the transaction QR code from your online wallet.

5. **Verify the Transaction:**  
   - Your offline wallet will display the scanned transaction. Double-check that everything is correct (destination address, amount, fee, etc.) to ensure your online wallet is secure.
   
6. **Sign the Transaction:**  
   - Press the `Sign` button on your offline wallet. It will generate a signature QR code.

7. **Publish the Transaction:**  
   - On your online wallet, press `Next` and scan the signature QR code.  
   - *Voila!* Your transaction is now published.

While this process may seem complicated, it is straightforward once you get the hang of it. For a better understanding, see the gif below.  
![transaction-gif](https://github.com/user-attachments/assets/265c3fd0-86b4-49cd-8b4c-09d107050100)
![signature-gif](https://github.com/user-attachments/assets/9d995c7c-677e-4650-af01-c72a20ef7392)




## Why Use Air Gap?
The air gap feature provides a similar experience to hardware wallets, but without the high-level anti-tamper protection and specific security features. It separates the signing and publishing processes, meaning that to lose your funds, both your online and offline wallets would need to be compromised, something that is quite difficult to achieve.

### Advantages Over Hardware Wallets:
- **Accessibility:** Many people have old Android phones that can be repurposed as secure offline wallets, eliminating the need for specialized hardware.
- **Customizability:** You can edit or compile the application yourself to ensure the program running on your offline wallet is secure—something not typically possible with hardware wallets.
- **Disguise:** An old phone in a drawer is less likely to be recognized as a wallet compared to a dedicated hardware wallet.

However, for the highest level of security, consider investing in a hardware wallet.
