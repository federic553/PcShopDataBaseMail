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
		for (Pc pc : listapc) {
			if (pc.getNome().equals(nome)) {
				for (PcSelezionato pcCom : pcSelezionati) {
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
	public String printCarrello(Model m1) {
		double somma = 0;
		for (PcSelezionato pc : pcSelezionati) {
			somma += pc.getPrezzo() * pc.getQnt();
		}
		m1.addAttribute("somma", somma);
		m1.addAttribute("lista", pcSelezionati);
		return ("carrello");
	}

	@PostMapping("/compra")
	public String fattura(Model m1) throws MessagingException {
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

	@GetMapping("/email")
	public String fatturaEmail(@RequestParam("index") String mail,Model m1) throws MessagingException{
		ArrayList<String> url = new ArrayList<String>();
		double somma = 0;
 
		StringBuilder message = new StringBuilder("<h1>Riepilogo acquisti:</h1>"
				+ "<table><thead><tr><th>Nome</th><th>Marca</th><th>Prezzo</th><th>Immagine</th><th>Quantità</th></tr><tbody>");
		for (PcSelezionato pc : pcSelezionati) {
			somma += pc.getPrezzo() * pc.getQnt();
			message.append("<tr><td>").append(pc.getNome()).append("</td><td>").append(pc.getMarca()).append("</td><td>").append(pc.getPrezzo()).append(" €</td>"
					+ "<td><img src='").append(pc.getUrl()).append("'style='width: 5vw; height: auto; margin: 10px;'/></td><td>").append(pc.getQnt()).append("</td></tr>");
//			url.add(pc.getUrl());
		}
		message.append("</tbody></table><h3>Totale: ").append(somma).append(" € </h3>");
		emS.sendEmail(mail ,"Grazie di aver acquistato da noi",message.toString());
		System.out.println("Acquisto avvenuto con successo");
		return "redirect:/";
	}
	/*@GetMapping("/email")
	public String fatturaEmail(@RequestParam("index") String mail,Model m1) throws MessagingException{
		ArrayList<String> url = new ArrayList<String>();
		
		double somma = 0;
		/*ArrayList<String> testo;
		testo.add(0,"I toui acquisti sono: ");
		for(int i=0;i<pcSelezionati.size();i++) {
			somma += pcSelezionati.get(i).getPrezzo() * pcSelezionati.get(i).getQnt();
			testo.add((i+1), "°"+pcSelezionati.get(i).getMarca()+" , "+pcSelezionati.get(i).getNome() +"\n"+pcSelezionati.get(i).getDescrizione()+"\n");
			url.add(pcSelezionati.get(i).getUrl());
		}*/
		/*String str="I toui acquisti sono: ";
		for (PcSelezionato pc : pcSelezionati) {
			somma += pc.getPrezzo() * pc.getQnt();
			str+="°"+pc.getMarca()+" , "+pc.getNome() +"\n"+pc.getDescrizione()+"\n";
			url.add(pc.getUrl());
		}
		emS.sendEmailWithImage(mail ,"Grazie di aver acquistato da noi",str,url);
		System.out.println("Acquisto avvenuto con successo");
		return "redirect:/";
	}*/
}