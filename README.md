## Quick Event

### Description 

Quick Event just a publish/subscribe framework just like "EventBus".

Feature:

- Sending message like "EventBus".

- Support muilt process.

- Support send request and get response.


### How to use

### I. Send Event

1. Define events

````
public static class MessageEvent { /* Additional fields if needed */ }
````

2. Prepare subscribers: Declare and annotate your subscribing method, optionally specify a thread mode

````
@Subscribe(threadMode = ThreadMode.MAIN)  
public void onMessageEvent(MessageEvent event) {/* Do something */};
````
Register and unregister your subscriber. For example on Android, activities and fragments should usually register according to their life cycle:

````
@Override
 public void onStart() {
      super.onStart();
      EventBus.getDefault().register(this);
}

 @Override
  public void onStop() {
       super.onStop();
       EventBus.getDefault().unregister(this);
}
````

3. Post events
````
EventBus.getDefault().post(new MessageEvent());
````

### I. Send requrst

1. Define service

````
@QuickService
public LoginResult login(Account account){
    if(account.getUsername().equals("admin") &&
            account.getPassword().equals("123456")){
        return new LoginResult(0,"Login Success");
    }else{
        return new LoginResult(-1, "Username or Password incorrect!");
    }
}
````

2. Send request and get response
````
Account account = new Account("admin", "123456");
LoginResult ret = QuickEvent.getDefault().request(account, LoginResult.class);
....
````

### Other

If you want run with IPC. Whatere sending evnet or request. Please run in thread.

with IPC:
```
(new Thread(new Runnable() {
    @Override
    public void run() {
        QuickEvent.getDefault().post("hello world");
    }
})).start();
```

without IPC:
```
QuickEvent.getDefault().post("hello world");
```


