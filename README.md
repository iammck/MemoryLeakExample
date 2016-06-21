#Eight Ways to leak memory

This application follows/reproduces a [blog post](http://blog.nimbledroid.com/2016/05/23/memory-leaks.html) by Tom Huzij. Thanks Tom! Memory management is very important and I am writing this to aid in my own understanding of the topic. Please read the actual article for a more comprehensive understanding.

Java uses garbage collection, reducing likelihood of segmentation fault crashing or by unreleased memory allocation bloating the heap. Still, android apps are capable of crashing as a result of out of memory errors.

Traditional memory leaks are caused by not freeing up memory (dealloc) before released references go out of scope.
 Logical memory leaks are the result of forgetting to release references to objects that are no longer needed by the app.
 
The lifecycle of an Activity is clearly defined. The onDestroy() method of an Activity is called at end-of-life and indicates that it is being destroyed. If this method completes but the Activity instance can be reached by a chain of strong references from a heap root, then the garbage collector cannot mark it for remobal from memory. As a result, we can define a leaked Activity object as one that persists beyond its natural lifecycle.

There are two fundamental situations causing memory leaks. The first is caused by a process global static object that exists regardless of the app's state and maintains a change of references to the Activity. The other category is caused when a thread that outlasts the Activity's lifetime neglects to clear a strong reference change to that Activity.

##1. Static Activities
The easiest way to leak an Activity is by defining a static variable inside the class definition of the Activity and then setting it to the running instance of that Activity. If this reference is not cleared before the Activity lifecycle completes, the Activity will be leaked. This is the result of the object representing the class of the Activity is static and remains loaded in memory for the entire runtime of the app. if this class object holds a reference to the Activity instance, it will not be eligible for garbage collection.

Consider

    private static Activity staticActivityLeak;

    View bStaticActivity = findViewById(R.id.bStaticActivity);
        bStaticActivity.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                staticActivityLeak = MainActivity.this;
                nextActivity();
            }
        });
This bit of code will produce a leaked reference to the Activity. Note that Android studio will warn the user about this.
    
    Do not place Android context classes in static fields; this is a memory leak (it also breaks instant run). A static field will leak contexts.

A similar situation would be implementing a singleton pattern holding onto a reference to the Activity. But this is dangerous and should be avoided.

##Static Views
But what if we have a particular View that takes a great deal of effort to instantiate but will remain unchanged across different lifetimes of same Activity? Well then let's make just that View static after instantiating it and attaching it to the View hierarchy. Now if our Activity is destroyed we should be able to release most of its memory.

Consider

    static View staticViewLeak;
    View bStaticViewLeak = findViewById(R.id.bStaticViewLeak);
    bStaticViewLeak.setOnClickListener(new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            staticViewLeak = findViewById(R.id.bStaticViewLeak);
            nextActivity();
        }
    });
The staticViewLeak field will hold onto the button view. The button view keeps a copy of its parent view (group?) and the parent keeps a reference to the context, activity.

##Inner Classes
Lets say we define a class inside the definition of our Activity's class, known as an Inner Class.

Consider

    static Object staticInnerClassLeak;
    class InnerClass{}
    
    View bStaticInnerClassLeak = findViewById(R.id.bStaticInnerClassLeak);
    bStaticInnerClassLeak.setOnClickListener(new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            staticInnerClassLeak = new InnerClass();
            nextActivity();
        }
    });
The static field holds a reference to the inner class. The innerclass holds a reference to the outer class, Activity.

##Anonymous Classes
Anonymous Classes will also maintain a reference to the class that they were declared inside. Therefore a leak can occur if you declare and instantiate an AsyncTask anonymously inside your Activity. If it continues to perform background work after the Activity has been destroyed, the reference to the Activity will persist and it will not be garbage collected until after the background task completes.

Consider

    View bAnonymousClassLeak = findViewById(R.id.bAnonymousClassLeak);
    bAnonymousClassLeak.setOnClickListener(new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            new AsyncTask<Void, Void, Void>(){

                @Override
                protected Void doInBackground(Void... params) {
                    SystemClock.sleep(20000);
                    return null;
                }
            }.execute();
            nextActivity();
        }
    });
In this example the container for the inner class is retained and the containing class is the activity. 

##Handlers
The same principle applies to background tasks declared anonymously by a Runnable object and queued up for execution by a Handler object. The Runnable object will implicitly reference the Activity it was declared in and will then be posted as a Message on the Handler's MessageQueue. As long as the message has not been handled before the Activity is destroyed, the chain of references will keep the Activity live in memory and will cause a leak.

    View bHandlerLeak = findViewById(R.id.bHandlerLeak);
    bHandlerLeak.setOnClickListener(new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            new MyHandler().postDelayed(new MyRunnable(), Long.MAX_VALUE >> 1);
            nextActivity();
        }
    });

    class MyHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
        }
    }

    class MyRunnable implements Runnable {
        @Override
        public void run() {
            while(true);
        }
    }
Checking the heap dump, the activity thread has a main looper. The main looper has a message queue. In the message queue is a MyUberHandler instance. The handler contains a reference to the Activity.

##Threads
This memory leak can also be achieved by Thread class as well as a Timer task.

    View bThreadLeak = findViewById(R.id.bThreadLeak);
    bThreadLeak.setOnClickListener(new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            new Thread() {
                @Override
                public void run() {
                    while(true);
                }
            }.start();
            nextActivity();
        }
    });

##Timer Tasks

    View bTimerTaskLeak = findViewById(R.id.bTimerTaskLeak);
    bTimerTaskLeak.setOnClickListener(new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            new Timer().schedule(new TimerTask() {
                @Override
                public void run() {
                    while (true);
                }
            }, Long.MAX_VALUE >> 1);
        }
    });

##Sensor Manager
There are system services that can be retrieved by a Context with a call to getSystemService(). These Services run in their own processes and assist applications by performing some sort of background work or interfacing to the device's hardware capabilities. If the Context wants to be notified every time an event occurs inside in Service, it needs to registers as a listener. This causes the Service to retain a reference to the activity. Neglecting to unregister the Activity as a listener will cause a leak

Consider

    View bSensorManagerLeak = findViewById(R.id.bSensorManagerLeak);
    bSensorManagerLeak.setOnClickListener(new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            SensorManager manager = (SensorManager) getSystemService(SENSOR_SERVICE);
            Sensor sensor = manager.getDefaultSensor(Sensor.TYPE_ALL);
            manager.registerListener(new MySensorListener(), sensor, SensorManager.SENSOR_DELAY_FASTEST);
            nextActivity();
        }
    });
    
    class MySensorListener implements SensorEventListener {
        @Override
        public void onSensorChanged(SensorEvent event) {
            Log.v("fdsa", "onSensorChanged()");
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {
            Log.v("fdsa", "onAccuracyChanged()");
        }

    }
The manager keeps a reference to the listener after the Activity is finished. The listener contains a implicit reference to its container Activity.
