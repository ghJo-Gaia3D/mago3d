package gaia3d.service.impl;

import gaia3d.domain.layer.LayerFileInfo;
import gaia3d.persistence.LayerFileInfoMapper;
import gaia3d.service.LayerFileInfoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class LayerFileInfoServiceImpl implements LayerFileInfoService {

	@Autowired
	private LayerFileInfoMapper layerFileInfoMapper;

	/**
	 * layer shape 파일 목록
	 * @return
	 */
	@Transactional(readOnly=true)
	public List<LayerFileInfo> getListLayerFileInfo(Integer layerId) {
		return layerFileInfoMapper.getListLayerFileInfo(layerId);
	}
	
	/**
	 * layerId에 해당하는 모든 파일 경로 목록 
	 * @param layerId
	 * @return
	 */
	@Transactional(readOnly=true)
	public List<String> getListLayerFilePath(Integer layerId) {
		return layerFileInfoMapper.getListLayerFilePath(layerId);
	}
	
	/**
	 * 파일 정보 취득
	 * @param layerFileInfoId
	 * @return
	 */
	@Transactional(readOnly=true)
	public LayerFileInfo getLayerFileInfo(Integer layerFileInfoId) {
		return layerFileInfoMapper.getLayerFileInfo(layerFileInfoId);
	}

	/**
	 * layer shape 파일 그룹 정보 취득
	 * @param layerFileInfoTeamId
	 * @return
	 */
	@Transactional(readOnly=true)
	public List<LayerFileInfo> getLayerFileInfoTeam(Integer layerFileInfoTeamId) {
		return layerFileInfoMapper.getLayerFileInfoTeam(layerFileInfoTeamId);
	}
	
	/**
	 * layer shape 파일 version
	 * @param layerFileInfoId
	 * @return
	 */
	@Transactional(readOnly=true)
	public Integer getLayerShapeFileVersion(Integer layerFileInfoId) {
		return layerFileInfoMapper.getLayerShapeFileVersion(layerFileInfoId);
	}
	
	/**
	 * 레이어 이력이 존재하는지 검사
	 * @param layerId
	 * @return
	 */
	@Transactional(readOnly=true)
	public boolean isLayerFileInfoExist(Integer layerId) {
		return layerFileInfoMapper.isLayerFileInfoExist(layerId);
	}
	
	/**
	 * 레이어 이력내 활성화 된 데이터 정보를 취득
	 * @param layerId
	 * @return
	 */
	@Transactional(readOnly=true)
	public LayerFileInfo getEnableLayerFileInfo(Integer layerId) {
		return layerFileInfoMapper.getEnableLayerFileInfo(layerId);
	}

	/**
	 * layer shape 파일 정보 수정
	 * @param layerFileInfo
	 * @return
	 */
	@Transactional
	public int updateLayerFileInfo(LayerFileInfo layerFileInfo) {
		return layerFileInfoMapper.updateLayerFileInfo(layerFileInfo);
	}

	/**
	 * team id 로 레이어 파일 이력을 삭제
	 * @param layerFileInfoTeamId
	 * @return
	 */
	@Transactional
	public int deleteLayerFileInfoByTeamId(Integer layerFileInfoTeamId) {
		return layerFileInfoMapper.deleteLayerFileInfoByTeamId(layerFileInfoTeamId);
	}
}
