/*
 * Copyright (C) 2016  Andrew Chow
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.achow101.bctalkaccountpricer.server;

import com.achow101.bctalkaccountpricer.shared.QueueRequest;

import javax.persistence.Entity;
import javax.persistence.Id;

/**
 * Created by Andy Chow on 4/18/2016.
 */

@Entity
public class QueueRequestDB {

    @Id private String token = "NO TOKEN";
    private int pos = -6;
    private boolean newRequest = true;
    private String ip;
    private long requestedTime;
    private String[] result;
    private int uid = 0;
    private boolean processing = false;
    private boolean done = false;
    private long expirationTime = 604800 * 2; // 2 weeks
    private boolean merch;
    private long completedTime;
    private boolean poll = false;

    public QueueRequestDB() {}

    public QueueRequestDB(QueueRequest req)
    {
        completedTime = req.getCompletedTime();
        done = req.isDone();
        expirationTime = req.getExpirationTime();
        ip = req.getIp();
        merch = req.isMerchant();
        poll = req.isPoll();
        processing = req.isProcessing();
        pos = req.getQueuePos();
        requestedTime = req.getRequestedTime();
        result = req.getResult();
        token = req.getToken();
        uid = req.getUid();
        newRequest = req.isNew();
    }

    public QueueRequest getQueueRequest()
    {
        QueueRequest req = new QueueRequest();
        req.setCompletedTime(completedTime);
        req.setDone(done);
        req.setExpirationTime(expirationTime);
        req.setIp(ip);
        req.setMerchant(merch);
        req.setPoll(poll);
        req.setProcessing(processing);
        req.setQueuePos(pos);
        req.setRequestedTime(requestedTime);
        req.setResult(result);
        req.setToken(token);
        req.setUid(uid);
        if(!newRequest)
            req.setOldReq();
        return req;
    }

    public int getQueuePos()
    {
        return pos;
    }

    public void setQueuePos(int qpos)
    {
        pos = qpos;
    }

    public String getIp()
    {
        return ip;
    }

    public void setIp(String ipAddress)
    {
        ip = ipAddress;
    }

    public void setOldReq()
    {
        newRequest = false;
    }

    public void setRequestedTime(long unixTime)
    {
        requestedTime = unixTime;
    }

    public long getRequestedTime()
    {
        return requestedTime;
    }

    public boolean isNew()
    {
        return newRequest;
    }

    public void setToken(String token)
    {
        this.token = token;
    }

    public String getToken()
    {
        return token;
    }

    public void setResult(String[] result)
    {
        this.result = result;
    }

    public String[] getResult()
    {
        return result;
    }

    public void setUid(int uid)
    {
        this.uid = uid;
    }

    public int getUid()
    {
        return uid;
    }

    public void setProcessing(boolean processing)
    {
        this.processing = processing;
    }

    public boolean isProcessing()
    {
        return processing;
    }

    public void setDone(boolean done)
    {
        this.done = done;
        setRequestedTime(System.currentTimeMillis() / 1000L);
    }

    public boolean isDone()
    {
        return done;
    }

    public void setExpirationTime(long secs)
    {
        expirationTime = secs;
    }

    public long getExpirationTime()
    {
        return expirationTime;
    }

    public boolean isExpired()
    {
        return (System.currentTimeMillis() / 1000L) > (completedTime + expirationTime);
    }

    public void setMerchant(boolean merch)
    {
        this.merch = merch;
    }

    public boolean isMerchant()
    {
        return merch;
    }

    public void setCompletedTime(long unixtime)
    {
        this.completedTime = unixtime;
    }

    public long getCompletedTime()
    {
        return completedTime;
    }

    public boolean isPoll()
    {
        return poll;
    }

    public void setPoll(boolean poll)
    {
        this.poll = poll;
    }
}
