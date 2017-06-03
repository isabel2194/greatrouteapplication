package com.greatRoute.services.impl;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONArray;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.geo.Point;
import org.springframework.stereotype.Service;

import com.greatRoute.converter.RutaConverter;
import com.greatRoute.converter.UserConverter;
import com.greatRoute.entity.Ruta;
import com.greatRoute.entity.Usuario;
import com.greatRoute.model.RutaModel;
import com.greatRoute.model.UserModel;
import com.greatRoute.repository.RutaRepository;
import com.greatRoute.services.RutaService;

@Service("rutaServiceImpl")
public class RutaServiceImpl implements RutaService {

	@Autowired
	@Qualifier("rutaRepository")
	private RutaRepository rutaRepository;

	@Autowired
	@Qualifier("rutaConverter")
	private RutaConverter converter;

	@Autowired
	@Qualifier("userConverter")
	private UserConverter converterUser;

	UserModel usuarioActivo;

	@Override
	public List<RutaModel> allRoutes(UserModel user) {
		List<RutaModel> rutas = new ArrayList<RutaModel>();
		Usuario userEntity = converterUser.modelToEntity(user);
		List<Ruta> entitiesRuta = rutaRepository.findByUsername(userEntity);
		if (entitiesRuta != null) {
			for (Ruta ruta : entitiesRuta)
				rutas.add(converter.entityToModel(ruta));
		}
		return rutas;
	}

	@Override
	public boolean guardarRuta(Ruta ruta) {
		if (rutaRepository.save(ruta) != null)
			return true;
		return false;
	}

	@Override
	public void borrarRuta(int id) {
		Ruta ruta = rutaRepository.findById(id);
		if(ruta!=null){
			File file = new File(ruta.getRecorrido());
			file.delete();
		}
		rutaRepository.delete(ruta);
	}

	@Override
	public boolean procesarRuta(String jsonResponse, UserModel user) {
		long distancia = 0;
		String origen = "";
		String destino = "";
		int tiempo = 0;

		try {
			JSONObject json = new JSONObject(jsonResponse);
			distancia = json.getLong("distance");
			origen = json.getJSONObject("start").getString("name");
			destino = json.getJSONObject("end").getString("name");
			tiempo = json.getInt("time");
			;
		} catch (JSONException e) {
			e.printStackTrace();
		}
		if (user != null) {
			String ruta = createFile(jsonResponse, user.getUsername());

			Usuario userEntity = converterUser.modelToEntity(user);

			return guardarRuta(new Ruta(ruta, distancia, userEntity, origen, destino, tiempo));
		} else
			return false;
	}

	private String createFile(String jsonResponse, String username) {
		FileWriter fichero = null;
		PrintWriter pw = null;
		String ruta = "recorrido_" + username + UUID.randomUUID() + ".json";
		try {
			fichero = new FileWriter(ruta);
			pw = new PrintWriter(fichero);
			pw.println(jsonResponse);

		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			try {
				if (null != fichero)
					fichero.close();
			} catch (Exception e2) {
				e2.printStackTrace();
			}
		}
		return ruta;
	}

	private void deleteFile(String name) {
		File fichero = new File(name);
		fichero.delete();
	}

	@Override
	public RutaModel obtenerRuta(int rutaId, UserModel user) {
		Usuario userEntity = converterUser.modelToEntity(user);
		Ruta ruta = rutaRepository.findByIdAndUsuario(rutaId, userEntity);
		if (ruta != null)
			return converter.entityToModel(ruta);
		return null;
	}

	@Override
	public String obtenerRecorrido(String path) {
		String cadena = "";
		File archivo = null;
		FileReader fr = null;
		BufferedReader br = null;
		try {
			archivo = new File(path);
			fr = new FileReader(archivo);
			br = new BufferedReader(fr);
			String linea;
			while ((linea = br.readLine()) != null)
				cadena += linea;
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			try {
				if (null != fr) {
					fr.close();
				}
			} catch (Exception e2) {
				e2.printStackTrace();
			}
		}
		return cadena;
	}

	@Override
	public boolean modificarRuta(int rutaId, String jsonResponse, UserModel usuarioActivo) {
		Ruta ruta = rutaRepository.findById(rutaId);
		if (ruta != null) {
			long distancia = 0;
			String origen = "";
			String destino = "";
			int tiempo = 0;

			try {
				JSONObject json = new JSONObject(jsonResponse);
				distancia = json.getLong("distance");
				origen = json.getJSONObject("start").getString("name");
				destino = json.getJSONObject("end").getString("name");
				tiempo = json.getInt("time");
				;
			} catch (JSONException e) {
				e.printStackTrace();
			}
			ruta.setDistancia(distancia);
			ruta.setDestino(destino);
			ruta.setOrigen(origen);
			ruta.setTiempoEstimado(tiempo);
			String rutaFile = createFile(jsonResponse, ruta.getUsuario().getUsername());
			deleteFile(ruta.getRecorrido());
			ruta.setRecorrido(rutaFile);
			rutaRepository.save(ruta);
			return true;
		}
		return false;
	}

	@Override
	public String createGPX(String jsonResponse) {

		String name = "pruebaGPX";
		String gpx = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><gpx version=\"1.0\"><name>" + name + "</name>";
		try {
			JSONObject json = new JSONObject(jsonResponse);

			Point origen = new Point(json.getJSONObject("start").getDouble("lat"),
					json.getJSONObject("start").getDouble("lng"));
			gpx += "<wpt lat=\"" + origen.getX() + "\" lon=\"" + origen.getY() + "\"><name>Inicio</name></wpt>";

			Point destino = new Point(json.getJSONObject("end").getDouble("lat"),
					json.getJSONObject("end").getDouble("lng"));
			gpx += "<wpt lat=\"" + destino.getX() + "\" lon=\"" + destino.getY() + "\"><name>Destino</name></wpt>";

			if (json.getJSONArray("path") != null) {
				gpx+="<trk><trkseg>";
				for (int i = 0; i < json.getJSONArray("path").length(); i++) {
					JSONArray waypoints = (JSONArray)json.getJSONArray("path").get(i);
					for (int j = 0; j < waypoints.length(); j++) {
						JSONObject array=(JSONObject)waypoints.get(j);
						gpx+="<trkpt lat=\""+array.getDouble("lat")+"\" lon=\""+array.getDouble("lng")+"\"></trkpt>";
					}
				}
				gpx+="</trkseg></trk>";
			}
			gpx+="</gpx>";
			
		} catch (JSONException e) {
			e.printStackTrace();
		}

		return gpx;
	}
}
