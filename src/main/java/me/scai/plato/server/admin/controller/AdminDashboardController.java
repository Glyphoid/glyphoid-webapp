package me.scai.plato.server.admin.controller;

import me.scai.plato.helpers.HandwritingEnginePool;
import me.scai.plato.server.admin.model.AdminDashboardModel;
import me.scai.plato.server.admin.model.WorkerAndClientInfo;
import me.scai.utilities.WorkerClientInfo;
import me.scai.utilities.WorkerPool;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.ModelAndView;

import java.util.*;

/**
 * Created by scai on 4/18/2015.
 */

@Controller
@RequestMapping("/admin")
public class AdminDashboardController {
    @RequestMapping("")
    public ModelAndView main(@ModelAttribute("adminDashboardModel") AdminDashboardModel adminDashboardModel) {
        // TODO: Thread safety?

        /* Get instance of worker pool */
        WorkerPool workerPool = HandwritingEnginePool.createOrGetWorkerPool();

        if (adminDashboardModel != null) {
            adminDashboardModel.setCurrNumWorkers(workerPool.getCurrNumWorkers());

            adminDashboardModel.setMaxNumWorkers(workerPool.getMaxNumWorkers());
            adminDashboardModel.setWorkerTimeoutMillis(workerPool.getWorkerTimeout());

            adminDashboardModel.setNumEverCreatedWorkers(workerPool.getNumEverCreatedWorkers());
            adminDashboardModel.setNumPurgedWorkers(workerPool.getNumPurgedWorkers());
            adminDashboardModel.setNumNormallyRemovedWorkers(workerPool.getNumNormallyRemovedWorkers());

            /* Get worker info */
            Map<String, WorkerClientInfo> workersInfo = workerPool.getWorkersClientInfo();
            LinkedList<WorkerAndClientInfo> workersAndClientsInfo = new LinkedList<>();
            LinkedList<String> workerIDs = new LinkedList<>();
            LinkedList<String> workerClientIPAddresses = new LinkedList<>();

            for (Map.Entry<String, WorkerClientInfo> entry : workersInfo.entrySet()) {
                String wkrID = entry.getKey();

                Date createdTimestamp = workerPool.getCreatedTimestamp(wkrID);
                Date lastUseTimestamp = workerPool.getLastUseTimestamp(wkrID);

                int messageCount                  = workerPool.getMessageCount(wkrID);
                float currentAverageMessageRate   = workerPool.getCurrentAverageMessageRate(wkrID);
                float effectiveAverageMessageRate = workerPool.getEffectiveAverageMessageRate(wkrID);

                workerIDs.add(wkrID);

                WorkerClientInfo wkrClientInfo = entry.getValue();

                WorkerAndClientInfo info = new WorkerAndClientInfo(
                        wkrID,
                        createdTimestamp,
                        lastUseTimestamp,
                        messageCount,
                        currentAverageMessageRate,
                        effectiveAverageMessageRate,
                        wkrClientInfo);

                String prevWorkerID = workerPool.getPreviousWorkerId(wkrID);

                if (prevWorkerID != null) {
                    info.setPrevWorkerID(prevWorkerID);
                }

                workersAndClientsInfo.add(info);

                workerClientIPAddresses.add(wkrClientInfo.getClientIPAddress().toString());
            }

            adminDashboardModel.setWorkersAndClientsInfo(workersAndClientsInfo);
            adminDashboardModel.setWorkerIDs(workerIDs);
            adminDashboardModel.setWorkerClientIPAddresses(workerClientIPAddresses);
        }

        return new ModelAndView("dashboard");
    }

    @RequestMapping(value = "", method = RequestMethod.POST, params = "update_worker_pool_settings")
    public ModelAndView updateWorkerPoolSettings(@ModelAttribute("adminDashboardModel") AdminDashboardModel adminDashboardModel) {
        /* Get instance of worker pool */
        WorkerPool workerPool = HandwritingEnginePool.createOrGetWorkerPool();

        workerPool.setMaxNumWorkers(adminDashboardModel.getMaxNumWorkers());
        workerPool.setWorkerTimeout(adminDashboardModel.getWorkerTimeoutMillis());

        return main(adminDashboardModel);
    }


}
