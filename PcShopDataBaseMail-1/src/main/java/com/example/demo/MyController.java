package com.example.demo;

import java.util.ArrayList;
import org.springframework.ui.Model;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import jakarta.mail.MessagingException;

import org.springframework.beans.factory.annotation.Autowired;

@Controller
public class MyController {

	@Autowired
	EmailService emS;
	
	PcJdbcTemplate db;
	ArrayList<Pc> listapc = new ArrayList<>();
	ArrayList<PcSelezionato> pcSelezionati = new ArrayList<>();

	/*
	 * Questo oggetto lo iniettiamo nel controller tramite costruttore
	 */
	@Autowired
	public MyController(PcJdbcTemplate db) {
		this.db = db;
		this.listapc = db.getLista();
	}

	@GetMapping("/")
	public String getIndex(Model m1) {

		m1.addAttribute("listapc", listapc);
		return "index";
	}

	@PostMapping("/process")
	public String buy(@RequestParam("nome") String nome, @RequestParam("num") int num) {

		if (num > 0) {
			// trovare l'oggetto con quel nome
			for (Pc pc : listapc) {
				if (pc.getNome().equals(nome)) {
					// se disponibile in magazzino
					if (pc.qntMagazzino >= num) {
						// aggiungerlo al carrello
						pcSelezionati.add(new PcSelezionato(pc, num));
						// diminuire la quantità disponibile e aumentare la quantità venduta
						// TO DO: aggiornare db
						// db.updateQnt(num, nome);
						// pc.qntMagazzino -= num;
						// pc.qntVenduta += num;
					}
				}
			}
		}
		return "redirect:/carrello";
	}
	
	@PostMapping("/rimuovi")
	public String removeProduct(@RequestParam("nome") String nome) {
		pcSelezionati.removeIf(pc -> pc.getNome().equals(nome));
		return "redirect:/carrello";
	}

	@PostMapping("/modifica")
	public String rimuovi(@RequestParam("nome") String nome, @RequestParam("num") int num) {
		for(Pc pc : listapc) {
			if(pc.getNome().equals(nome)) {
				for(PcSelezionato pcCom : pcSelezionati) {
					if (pcCom.getNome().equals(nome) && pc.qntMagazzino >= num) {
						pcCom.setQnt(num);
						break;
					}
				}				
			}
		}
		return "redirect:/carrello";
	}

	@GetMapping("/carrello")
	public String printCarrello(Model m1){
		double somma = 0;
		for (PcSelezionato pc : pcSelezionati) {
			somma += pc.getPrezzo() * pc.getQnt();
		}
		m1.addAttribute("somma", somma);
		m1.addAttribute("lista", pcSelezionati);
		return ("carrello");
	}
	@PostMapping("/compra")
	public String fattura(Model m1) throws MessagingException{
		ArrayList<String> url = new ArrayList<String>();
		double somma = 0;
		for (PcSelezionato pc : pcSelezionati) {
			somma += pc.getPrezzo() * pc.getQnt();
			db.updateQnt(pc.getQnt(), pc.getNome());
			url.add(pc.getUrl());
		}
		m1.addAttribute("somma", somma);
		m1.addAttribute("lista", pcSelezionati);
		return ("fattura");
	}
	//@ResponseBody
	@GetMapping("/email")
	public String fatturaEmail(@RequestParam("index") String mail,Model m1) throws MessagingException{
		ArrayList<String> url = new ArrayList<String>();
		double somma = 0;
		String str="I toui acquisti sono: ";
		for (PcSelezionato pc : pcSelezionati) {
			somma += pc.getPrezzo() * pc.getQnt();
			str+="°"+pc.getMarca()+" , "+pc.getNome() +"\n"+pc.getDescrizione()+"\n";
			url.add(pc.getUrl());
		}
		emS.sendEmailWithImage(mail ,"Grazie di aver acquistato da noi",str,url);
		System.out.println("Acquisto avvenuto con successo");
		return "redirect:/";
	}
}