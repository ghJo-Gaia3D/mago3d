package gaia3d.controller.view;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import gaia3d.controller.AuthorizationController;
import gaia3d.domain.Key;
import gaia3d.domain.ShapeFileExt;
import gaia3d.domain.common.Pagination;
import gaia3d.domain.layer.Layer;
import gaia3d.domain.layer.LayerFileInfo;
import gaia3d.domain.layer.LayerGroup;
import gaia3d.domain.layer.LayerInsertType;
import gaia3d.domain.policy.GeoPolicy;
import gaia3d.domain.policy.Policy;
import gaia3d.domain.role.RoleKey;
import gaia3d.domain.rule.Rule;
import gaia3d.domain.user.UserSession;
import gaia3d.rule.RuleType;
import gaia3d.service.*;
import gaia3d.support.LogMessageSupport;
import gaia3d.support.SQLInjectSupport;
import gaia3d.utils.DateUtils;
import gaia3d.utils.FormatUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.ObjectUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Controller
@RequestMapping("/layer")
public class LayerController implements AuthorizationController {

    @Autowired
    private GeoPolicyService geoPolicyService;
    @Autowired
    private LayerService layerService;
    @Autowired
    private LayerFileInfoService layerFileInfoService;
    @Autowired
    private LayerGroupService layerGroupService;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private PolicyService policyService;
    @Autowired
    private RuleService ruleService;

    // 파일 copy 시 버퍼 사이즈
    public static final int BUFFER_SIZE = 8192;

    /**
     * layer 목록
     */
    @GetMapping(value = "/list")
    public String list(HttpServletRequest request, @RequestParam(defaultValue = "1") String pageNo, Layer layer, Model model) {
        layer.setSearchWord(SQLInjectSupport.replaceSqlInection(layer.getSearchWord()));
        layer.setOrderWord(SQLInjectSupport.replaceSqlInection(layer.getOrderWord()));

        log.info("@@ layer = {}", layer);

        String roleCheckResult = roleValidate(request);
        if(roleCheckResult != null) return roleCheckResult;

        String today = DateUtils.getToday(FormatUtils.YEAR_MONTH_DAY);
        if (ObjectUtils.isEmpty(layer.getStartDate())) {
            layer.setStartDate(today.substring(0, 4) + DateUtils.START_DAY_TIME);
        } else {
            layer.setStartDate(layer.getStartDate().substring(0, 8) + DateUtils.START_TIME);
        }
        if (ObjectUtils.isEmpty(layer.getEndDate())) {
            layer.setEndDate(today + DateUtils.END_TIME);
        } else {
            layer.setEndDate(layer.getEndDate().substring(0, 8) + DateUtils.END_TIME);
        }

        Long totalCount = layerService.getLayerTotalCount(layer);
        Pagination pagination = new Pagination(request.getRequestURI(), layer.getParameters(),
                totalCount, Long.parseLong(pageNo), layer.getListCounter());
        layer.setOffset(pagination.getOffset());
        layer.setLimit(pagination.getPageRows());

        List<Layer> layerList = new ArrayList<>();
        if (totalCount > 0L) {
            layerList = layerService.getListLayer(layer);
        }

        model.addAttribute(pagination);
        model.addAttribute("layer", layer);
        model.addAttribute("totalCount", totalCount);
        model.addAttribute("layerList", layerList);

        return "/layer/list";
    }

    /**
     * layer 등록
     *
     * @param model
     * @return
     */
    @GetMapping(value = "/input")
    public String input(HttpServletRequest request, Model model) {
        String roleCheckResult = roleValidate(request);
        if(roleCheckResult != null) return roleCheckResult;

        Policy policy = policyService.getPolicy();
        List<LayerGroup> layerGroupList = layerGroupService.getListLayerGroup();
        List<Rule> ruleList = ruleService.getListAllRuleByRuleType(Rule.builder().ruleType(RuleType.LAYER.toString().toLowerCase()).build());

        model.addAttribute("policy", policy);
        model.addAttribute("layer", new Layer());
        model.addAttribute("layerGroupList", layerGroupList);
        model.addAttribute("ruleList", ruleList);

        return "/layer/input";
    }

    /**
     * layer 수정
     *
     * @param model
     * @return
     */
    @GetMapping(value = "/modify")
    public String modify(HttpServletRequest request, @RequestParam Integer layerId, Model model) {
        String roleCheckResult = roleValidate(request);
        if(roleCheckResult != null) return roleCheckResult;

        Policy policy = policyService.getPolicy();
        Layer layer = layerService.getLayer(layerId);
        List<LayerGroup> layerGroupList = layerGroupService.getListLayerGroup();
        List<Rule> ruleList = ruleService.getListAllRuleByRuleType(Rule.builder().ruleType(RuleType.LAYER.toString().toLowerCase()).build());

        model.addAttribute("policy", policy);
        model.addAttribute("layer", layer);
        model.addAttribute("layerGroupList", layerGroupList);
        model.addAttribute("ruleList", ruleList);

        // 파일업로드로 레이어를 등록한 경우
        if (LayerInsertType.UPLOAD == LayerInsertType.valueOf(layer.getLayerInsertType().toUpperCase())) {
            List<LayerFileInfo> layerFileInfoList = layerFileInfoService.getListLayerFileInfo(layerId);
            LayerFileInfo layerFileInfo = new LayerFileInfo();
            for (LayerFileInfo fileInfo : layerFileInfoList) {
                if (ShapeFileExt.SHP == ShapeFileExt.valueOf(fileInfo.getFileExt().toUpperCase())) {
                    layerFileInfo = fileInfo;
                }
            }

            model.addAttribute("layerFileInfo", layerFileInfo);
            model.addAttribute("layerFileInfoList", layerFileInfoList);
            model.addAttribute("layerFileInfoListSize", layerFileInfoList.size());

            return "/layer/modify-upload";
        } else { //geoserver 레이어를 등록한 경우 
            return "/layer/modify-geoserver";
        }
    }

    /**
     * layer 지도 보기
     *
     * @param model
     * @return
     */
    @GetMapping(value = "/{layerId}/map")
    public String viewLayerMap(HttpServletRequest request, @PathVariable Integer layerId, Integer layerFileInfoId, Model model) {
        log.info("@@ layerId = {}, layerFileInfoId = {}", layerId, layerFileInfoId);

        GeoPolicy policy = geoPolicyService.getGeoPolicy();
        Layer layer = layerService.getLayer(layerId);
        Integer versionId = 0;
        if (layerFileInfoId != null) {
            versionId = layerFileInfoService.getLayerShapeFileVersion(layerFileInfoId);
        }

        String policyJson = "";
        String layerJson = "";
        try {
            policyJson = objectMapper.writeValueAsString(policy);
            layerJson = objectMapper.writeValueAsString(layer);
        } catch (JsonProcessingException e) {
            LogMessageSupport.printMessage(e, "@@ JsonProcessingException. message = {}", e.getClass().getName());
        }

        model.addAttribute("policyJson", policyJson);
        model.addAttribute("layerJson", layerJson);
        model.addAttribute("versionId", versionId);

        return "/layer/popup-map";
    }

    private String roleValidate(HttpServletRequest request) {
        UserSession userSession = (UserSession) request.getSession().getAttribute(Key.USER_SESSION.name());
        int httpStatusCode = getRoleStatusCode(userSession.getUserGroupId(), RoleKey.ADMIN_LAYER_MANAGE.name());
        if (httpStatusCode > 200) {
            log.info("@@ httpStatusCode = {}", httpStatusCode);
            request.setAttribute("httpStatusCode", httpStatusCode);
            return "/error/error";
        }

        return null;
    }
}
