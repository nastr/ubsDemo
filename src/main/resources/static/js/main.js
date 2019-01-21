'use strict';

var messageArea = document.querySelector('.message-container');

var stompClient = null;

function connect(event) {
    var socket = new SockJS('/alerts');
    stompClient = Stomp.over(socket);

    stompClient.connect({}, onConnected, onError);
    // event.preventDefault();
    console.log(stompClient.toString());
}


function onConnected() {
    stompClient.subscribe('/topic/public', onMessageReceived);
}


function onError(error) {
    messageArea.textContent = 'Could not connect to WebSocket server. Please refresh this page to try again!';
    messageArea.style.color = 'red';
}


function onMessageReceived(payload) {
    var message = JSON.parse(payload.body);

    var messageElement = document.createElement('li');

    messageElement.classList.add('message');

    var textElement = document.createElement('p');
    var messageText = document.createTextNode(message.limit + '\t\t' + new Date(message.timestamp).toLocaleDateString());
    textElement.appendChild(messageText);

    messageElement.appendChild(textElement);

    messageArea.appendChild(messageElement);
    messageArea.scrollTop = messageArea.scrollHeight;
}

connect();
// usernameForm.addEventListener('submit', connect, true);
